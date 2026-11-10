variable "name" {
  description = "Nome da fila; a DLQ recebe o sufixo -dlq"
  type        = string
}

variable "visibility_timeout_seconds" {
  description = "Tempo que a mensagem fica invisível durante o processamento; pelo menos 6x o timeout da Lambda consumidora"
  type        = number
}

variable "message_retention_seconds" {
  description = "Retenção na fila principal"
  type        = number
  default     = 345600 # 4 dias
}

variable "dlq_retention_seconds" {
  description = "Retenção na DLQ (maior que a da fila principal)"
  type        = number
  default     = 1209600 # 14 dias, o máximo
}

variable "max_receive_count" {
  description = "Tentativas antes de mandar a mensagem para a DLQ"
  type        = number
  default     = 3
}
