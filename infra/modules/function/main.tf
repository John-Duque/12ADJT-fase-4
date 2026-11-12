# Lambda Java 21 com SnapStart, papel IAM próprio (prefixo feedback-), logs com retenção curta e alias live.
# O SnapStart só vale para versões publicadas: cada deploy publica uma versão e move o alias para ela.

data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  name               = var.role_name
  description        = "Papel da função ${var.name}"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

resource "aws_cloudwatch_log_group" "this" {
  name              = "/aws/lambda/${var.name}"
  retention_in_days = var.log_retention_days
}

# Só grava no próprio grupo de logs (o grupo é criado acima, então não precisa de logs:CreateLogGroup)
data "aws_iam_policy_document" "logs" {
  statement {
    sid       = "Logs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.this.arn}:*"]
  }
}

data "aws_iam_policy_document" "this" {
  source_policy_documents = [data.aws_iam_policy_document.logs.json, var.policy_json]
}

resource "aws_iam_role_policy" "this" {
  name   = "permissoes"
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.this.json
}

resource "aws_lambda_function" "this" {
  function_name = var.name
  description   = var.description
  role          = aws_iam_role.this.arn
  runtime       = "java21"
  handler       = var.handler
  memory_size   = var.memory_size
  timeout       = var.timeout

  filename         = var.zip_path
  source_code_hash = filebase64sha256(var.zip_path)
  publish          = true

  # -1 = sem reserva (usa o limite da conta)
  reserved_concurrent_executions = var.reserved_concurrency

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = var.environment
  }

  depends_on = [aws_iam_role_policy.this, aws_cloudwatch_log_group.this]
}

data "aws_region" "current" {}

# A versão nova só atende depois que o snapshot do SnapStart fica pronto (alguns minutos).
# Espera com o AWS CLI antes de mover o alias, para o deploy não deixar a função fora do ar.
resource "terraform_data" "version_ready" {
  input = aws_lambda_function.this.version

  provisioner "local-exec" {
    command = "aws lambda wait published-version-active --function-name ${aws_lambda_function.this.function_name} --qualifier ${aws_lambda_function.this.version} --region ${data.aws_region.current.region}"
  }
}

resource "aws_lambda_alias" "live" {
  name             = "live"
  function_name    = aws_lambda_function.this.function_name
  function_version = terraform_data.version_ready.output
}

# Endereço HTTPS público (sem custo, no lugar do API Gateway); só HTTPS, a AWS não aceita HTTP
resource "aws_lambda_function_url" "this" {
  count = var.public_url ? 1 : 0

  function_name      = aws_lambda_function.this.function_name
  qualifier          = aws_lambda_alias.live.name
  authorization_type = "NONE"
}

# Function URL sem autenticação precisa das duas permissões: chamar a URL e invocar a função por ela
resource "aws_lambda_permission" "url" {
  count = var.public_url ? 1 : 0

  statement_id           = "FunctionUrlPublica"
  action                 = "lambda:InvokeFunctionUrl"
  function_name          = aws_lambda_function.this.function_name
  qualifier              = aws_lambda_alias.live.name
  principal              = "*"
  function_url_auth_type = "NONE"
}

resource "aws_lambda_permission" "url_invoke" {
  count = var.public_url ? 1 : 0

  statement_id             = "FunctionUrlInvoca"
  action                   = "lambda:InvokeFunction"
  function_name            = aws_lambda_function.this.function_name
  qualifier                = aws_lambda_alias.live.name
  principal                = "*"
  invoked_via_function_url = true
}
