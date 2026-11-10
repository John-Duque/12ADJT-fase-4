variable "aws_region" {
  description = "Região do projeto na conta simplificada (ADR 0004)"
  type        = string
  default     = "us-east-2"
}

variable "project" {
  description = "Nome do projeto, usado nas tags"
  type        = string
  default     = "12adjt-fase-4"
}

variable "environment" {
  description = "Nome do ambiente, usado nas tags"
  type        = string
  default     = "dev"
}

variable "admin_emails" {
  description = "E-mails dos administradores por apelido; no CI vem do secret ADMIN_EMAILS (nunca fica no código)"
  type        = map(string)
  sensitive   = true
  default     = {}
}
