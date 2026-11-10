# Ambiente dev: chama os módulos de infra/modules (banco, fila, e-mail, funções, agendamento e monitoramento)

data "aws_region" "current" {}

locals {
  # Timeout da Lambda que consome a fila; a visibilidade da fila é 6x esse valor
  notifier_timeout_seconds = 30
}

module "feedback_table" {
  source = "../../modules/table"

  name = "feedbacks"
}

module "critical_queue" {
  source = "../../modules/queue"

  name                       = "critical-feedbacks"
  visibility_timeout_seconds = 6 * local.notifier_timeout_seconds
}
