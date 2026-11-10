# Agenda do EventBridge Scheduler que chama uma Lambda (sempre gratuito até 14 milhões de invocações por mês).

data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["scheduler.amazonaws.com"]
    }

    # Só agendas desta conta assumem o papel
    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_iam_role" "this" {
  name               = var.role_name
  description        = "Papel da agenda ${var.name}"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

data "aws_iam_policy_document" "invoke" {
  statement {
    sid       = "InvocaAFuncao"
    actions   = ["lambda:InvokeFunction"]
    resources = [var.target_arn]
  }
}

resource "aws_iam_role_policy" "this" {
  name   = "invoca-funcao"
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.invoke.json
}

resource "aws_scheduler_schedule" "this" {
  name                         = var.name
  description                  = var.description
  schedule_expression          = var.schedule_expression
  schedule_expression_timezone = var.timezone

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = var.target_arn
    role_arn = aws_iam_role.this.arn
    input    = var.input

    # Poucas tentativas: cada uma pode mandar um e-mail
    retry_policy {
      maximum_retry_attempts       = var.maximum_retry_attempts
      maximum_event_age_in_seconds = 3600
    }
  }
}
