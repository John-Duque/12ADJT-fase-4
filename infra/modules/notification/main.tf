# Tópico SNS com uma assinatura de e-mail por administrador (sempre gratuito até 1.000 e-mails/mês).
# Cada administrador confirma a assinatura pelo link que recebe por e-mail.

resource "aws_sns_topic" "this" {
  name = var.name

  # Chave gerenciada pela AWS (sem custo mensal); null deixa o tópico sem criptografia
  kms_master_key_id = var.encrypted ? "alias/aws/sns" : null
}

resource "aws_sns_topic_subscription" "email" {
  # Só os apelidos aparecem no plan; os e-mails ficam ocultos (variável sensível)
  for_each = nonsensitive(toset(keys(var.emails)))

  topic_arn = aws_sns_topic.this.arn
  protocol  = "email"
  endpoint  = var.emails[each.key]
}
