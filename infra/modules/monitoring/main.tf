# Dashboard do CloudWatch (3 são sempre gratuitos, até 50 métricas cada) e consultas salvas do Logs Insights.
# As consultas leem os logs das funções, dentro dos 5 GB/mês gratuitos do CloudWatch Logs.

data "aws_region" "current" {}

locals {
  region = data.aws_region.current.region

  # Último trecho de @log (/aws/lambda/<função>): só o nome da função, sem o ID da conta
  parse_function = "parse @log /(?<funcao>[^\\/]+)$/"

  queries = {
    "rastrear-feedback-por-id" = <<-EOT
      fields @timestamp, @message
      | ${local.parse_function}
      | filter @message like "COLE-O-ID-DO-FEEDBACK-AQUI"
      | display @timestamp, funcao, @message
      | sort @timestamp asc
    EOT

    "erros" = <<-EOT
      fields @timestamp, @message
      | ${local.parse_function}
      | filter @message like /ERROR|Exception|Task timed out/
      | display @timestamp, funcao, @message
      | sort @timestamp desc
      | limit 50
    EOT

    "duracao-por-funcao" = <<-EOT
      filter @type = "REPORT"
      | ${local.parse_function}
      | stats count(*) as execucoes, avg(@duration) as duracaoMediaMs, max(@duration) as duracaoMaximaMs, max(@maxMemoryUsed / 1000 / 1000) as memoriaMaximaMB by funcao
    EOT
  }

  # Uma linha por função para cada métrica da Lambda
  lambda_metrics = {
    for metric in ["Invocations", "Errors", "Throttles", "Duration"] :
    metric => [for name in var.function_names : ["AWS/Lambda", metric, "FunctionName", name, { label = name }]]
  }

  sns_metrics = flatten([
    for topic in var.topic_names : [
      ["AWS/SNS", "NumberOfNotificationsDelivered", "TopicName", topic, { label = "${topic}: entregues" }],
      ["AWS/SNS", "NumberOfNotificationsFailed", "TopicName", topic, { label = "${topic}: falhas" }],
    ]
  ])

  log_sources = join(" | ", [for name in var.log_group_names : "SOURCE '${name}'"])

  widgets = [
    {
      type = "text", x = 0, y = 0, width = 24, height = 2
      properties = {
        markdown = "## Plataforma de feedback\nFunções Lambda, fila de críticos, e-mails (SNS) e banco. Consultas salvas no Logs Insights, pasta **${var.query_folder}**: rastrear um feedback pelo id, erros e duração por função."
      }
    },
    {
      type = "metric", x = 0, y = 2, width = 8, height = 6
      properties = {
        title   = "Invocações", region = local.region, view = "timeSeries", stat = "Sum", period = 300
        metrics = local.lambda_metrics["Invocations"]
      }
    },
    {
      type = "metric", x = 8, y = 2, width = 8, height = 6
      properties = {
        title   = "Erros", region = local.region, view = "timeSeries", stat = "Sum", period = 300
        metrics = local.lambda_metrics["Errors"]
      }
    },
    {
      type = "metric", x = 16, y = 2, width = 8, height = 6
      properties = {
        title   = "Throttles (limite de execuções)", region = local.region, view = "timeSeries", stat = "Sum", period = 300
        metrics = local.lambda_metrics["Throttles"]
      }
    },
    {
      type = "metric", x = 0, y = 8, width = 12, height = 6
      properties = {
        title   = "Duração média (ms)", region = local.region, view = "timeSeries", stat = "Average", period = 300
        metrics = local.lambda_metrics["Duration"]
      }
    },
    {
      type = "metric", x = 12, y = 8, width = 12, height = 6
      properties = {
        title   = "Duração máxima (ms)", region = local.region, view = "timeSeries", stat = "Maximum", period = 300
        metrics = local.lambda_metrics["Duration"]
      }
    },
    {
      type = "metric", x = 0, y = 14, width = 8, height = 6
      properties = {
        title = "Fila de críticos", region = local.region, view = "timeSeries", stat = "Sum", period = 300
        metrics = [
          ["AWS/SQS", "NumberOfMessagesSent", "QueueName", var.queue_name, { label = "enviadas" }],
          ["AWS/SQS", "NumberOfMessagesDeleted", "QueueName", var.queue_name, { label = "processadas" }],
        ]
      }
    },
    {
      type = "metric", x = 8, y = 14, width = 8, height = 6
      properties = {
        title = "Mensagens esperando na fila", region = local.region, view = "timeSeries", stat = "Maximum", period = 300
        metrics = [
          ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", var.queue_name, { label = "visíveis" }],
          ["AWS/SQS", "ApproximateAgeOfOldestMessage", "QueueName", var.queue_name, { label = "idade da mais antiga (s)", yAxis = "right" }],
        ]
      }
    },
    {
      type = "metric", x = 16, y = 14, width = 8, height = 6
      properties = {
        title = "DLQ: mensagens com falha", region = local.region, view = "singleValue", stat = "Maximum", period = 300, sparkline = true
        metrics = [
          ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", var.dlq_name, { label = var.dlq_name }],
        ]
      }
    },
    {
      type = "metric", x = 0, y = 20, width = 12, height = 6
      properties = {
        title   = "E-mails (SNS)", region = local.region, view = "timeSeries", stat = "Sum", period = 300
        metrics = local.sns_metrics
      }
    },
    {
      type = "metric", x = 12, y = 20, width = 12, height = 6
      properties = {
        title = "Capacidade consumida no DynamoDB (soma em 5 min)", region = local.region, view = "timeSeries", stat = "Sum", period = 300
        metrics = [
          ["AWS/DynamoDB", "ConsumedWriteCapacityUnits", "TableName", var.table_name, { label = "escrita na tabela" }],
          ["AWS/DynamoDB", "ConsumedReadCapacityUnits", "TableName", var.table_name, "GlobalSecondaryIndexName", var.day_index_name, { label = "leitura no índice ${var.day_index_name}" }],
        ]
      }
    },
    {
      type = "log", x = 0, y = 26, width = 24, height = 6
      properties = {
        title  = "Erros recentes nas funções"
        region = local.region
        view   = "table"
        query  = "${local.log_sources} | ${replace(trimspace(local.queries["erros"]), "\n", " ")}"
      }
    },
  ]
}

resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = var.dashboard_name
  dashboard_body = jsonencode({ widgets = local.widgets })
}

resource "aws_cloudwatch_query_definition" "this" {
  for_each = local.queries

  name            = "${var.query_folder}/${each.key}"
  log_group_names = var.log_group_names
  query_string    = trimspace(each.value)
}
