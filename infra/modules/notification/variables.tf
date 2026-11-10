variable "name" {
  description = "Nome do tópico"
  type        = string
}

variable "emails" {
  description = "E-mails dos administradores, por apelido (ex.: { john = \"admin@exemplo.com\" })"
  type        = map(string)
  sensitive   = true
  default     = {}
}

variable "encrypted" {
  description = "Criptografa o tópico com a chave alias/aws/sns (o CloudWatch não publica em tópico com essa chave)"
  type        = bool
  default     = true
}
