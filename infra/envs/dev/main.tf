# Ambiente dev: chama os módulos de infra/modules (banco, fila, e-mail, funções, agendamento e monitoramento)

data "aws_region" "current" {}

locals {
  # Timeout da Lambda que consome a fila; a visibilidade da fila é 6x esse valor
  notifier_timeout_seconds = 30

  # function.zip de cada função, gerado por mvn package -Plambda antes do Terraform
  functions_dir = "${path.root}/../../../functions"

  # JIT só no nível 1: menos tempo de inicialização, que pesa mais que o pico de desempenho em funções curtas
  java_tool_options = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

  quarkus_http_handler  = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  quarkus_funqy_handler = "io.quarkus.funqy.lambda.FunqyStreamHandler::handleRequest"
}

module "feedback_table" {
  source = "../../modules/table"

  name = "feedbacks"
}

module "critical_queue" {
  source = "../../modules/queue"

  name                       = "critical-feedbacks"
  visibility_timeout_seconds = 6 * local.notifier_timeout_seconds
  policy_json                = data.aws_iam_policy_document.critical_queue.json
}

# Na política da própria fila: só a ingestion publica e só o notifier consome
data "aws_iam_policy_document" "critical_queue" {
  statement {
    sid       = "IngestionPublica"
    actions   = ["sqs:SendMessage"]
    resources = [module.critical_queue.arn]

    principals {
      type        = "AWS"
      identifiers = [module.ingestion_function.role_arn]
    }
  }

  statement {
    sid       = "NotifierConsome"
    actions   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes", "sqs:ChangeMessageVisibility"]
    resources = [module.critical_queue.arn]

    principals {
      type        = "AWS"
      identifiers = [module.notifier_function.role_arn]
    }
  }
}

module "critical_topic" {
  source = "../../modules/notification"

  name   = "feedbacks-criticos"
  emails = var.admin_emails
}

module "report_topic" {
  source = "../../modules/notification"

  name   = "relatorio-semanal"
  emails = var.admin_emails
}

# Quem publica no tópico criptografado com a chave aws/sns precisa usar a chave, só pelo SNS
data "aws_iam_policy_document" "sns_kms" {
  statement {
    sid       = "ChaveDoSns"
    actions   = ["kms:GenerateDataKey*", "kms:Decrypt"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["sns.${data.aws_region.current.region}.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "ingestion" {
  statement {
    sid       = "GravaFeedbacks"
    actions   = ["dynamodb:PutItem"]
    resources = [module.feedback_table.arn]
  }

  statement {
    sid       = "PublicaCriticos"
    actions   = ["sqs:SendMessage"]
    resources = [module.critical_queue.arn]
  }
}

module "ingestion_function" {
  source = "../../modules/function"

  name        = "feedback-ingestion"
  role_name   = "feedback-ingestion"
  description = "POST /avaliacao: valida, grava no DynamoDB e publica os críticos na fila"
  handler     = local.quarkus_http_handler
  zip_path    = "${local.functions_dir}/feedback-ingestion/target/function.zip"
  timeout     = 15
  public_url  = true
  policy_json = data.aws_iam_policy_document.ingestion.json

  # Limite de requisições simultâneas da URL pública (sem custo); -1 = sem reserva
  reserved_concurrency = var.ingestion_reserved_concurrency

  environment = {
    FEEDBACK_TABLE     = module.feedback_table.name
    CRITICAL_QUEUE_URL = module.critical_queue.url
    JAVA_TOOL_OPTIONS  = local.java_tool_options
  }
}

data "aws_iam_policy_document" "notifier" {
  source_policy_documents = [data.aws_iam_policy_document.sns_kms.json]

  statement {
    sid       = "ConsomeCriticos"
    actions   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes", "sqs:ChangeMessageVisibility"]
    resources = [module.critical_queue.arn]
  }

  statement {
    sid       = "EnviaEmailDeUrgencia"
    actions   = ["sns:Publish"]
    resources = [module.critical_topic.arn]
  }
}

module "notifier_function" {
  source = "../../modules/function"

  name        = "critical-notifier"
  role_name   = "feedback-critical-notifier"
  description = "Consome a fila de críticos e envia o e-mail de urgência pelo SNS"
  handler     = local.quarkus_funqy_handler
  zip_path    = "${local.functions_dir}/critical-notifier/target/function.zip"
  timeout     = local.notifier_timeout_seconds
  policy_json = data.aws_iam_policy_document.notifier.json

  environment = {
    CRITICAL_TOPIC_ARN = module.critical_topic.arn
    JAVA_TOOL_OPTIONS  = local.java_tool_options
  }
}

# Fila → notifier: só as mensagens que falharem voltam para a fila (e, depois de 3 vezes, vão para a DLQ)
resource "aws_lambda_event_source_mapping" "critical_queue" {
  event_source_arn        = module.critical_queue.arn
  function_name           = module.notifier_function.alias_arn
  batch_size              = 10
  function_response_types = ["ReportBatchItemFailures"]

  # No máximo 2 execuções do notifier ao mesmo tempo (o mínimo que a AWS aceita)
  scaling_config {
    maximum_concurrency = 2
  }
}

data "aws_iam_policy_document" "report" {
  source_policy_documents = [data.aws_iam_policy_document.sns_kms.json]

  statement {
    sid       = "ConsultaPorDia"
    actions   = ["dynamodb:Query"]
    resources = ["${module.feedback_table.arn}/index/${module.feedback_table.day_index_name}"]
  }

  statement {
    sid       = "EnviaRelatorio"
    actions   = ["sns:Publish"]
    resources = [module.report_topic.arn]
  }
}

module "report_function" {
  source = "../../modules/function"

  name        = "weekly-report"
  role_name   = "feedback-weekly-report"
  description = "Consolida a semana a partir do DynamoDB e envia o relatório pelo SNS"
  handler     = local.quarkus_funqy_handler
  zip_path    = "${local.functions_dir}/weekly-report/target/function.zip"
  timeout     = 60
  policy_json = data.aws_iam_policy_document.report.json

  environment = {
    FEEDBACK_TABLE     = module.feedback_table.name
    FEEDBACK_DAY_INDEX = module.feedback_table.day_index_name
    REPORT_TOPIC_ARN   = module.report_topic.arn
    JAVA_TOOL_OPTIONS  = local.java_tool_options
  }
}

# Toda segunda às 08:00 de São Paulo: relatório da semana anterior (segunda a domingo)
module "report_schedule" {
  source = "../../modules/schedule"

  name                = "feedback-weekly-report"
  description         = "Gera e envia o relatório semanal de avaliações"
  role_name           = "feedback-scheduler"
  schedule_expression = "cron(0 8 ? * MON *)"
  timezone            = "America/Sao_Paulo"
  target_arn          = module.report_function.alias_arn
}
