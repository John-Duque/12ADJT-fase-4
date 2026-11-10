# Ambiente dev: chama os módulos de infra/modules (banco, fila, e-mail, funções, agendamento e monitoramento)

data "aws_region" "current" {}

module "feedback_table" {
  source = "../../modules/table"

  name = "feedbacks"
}
