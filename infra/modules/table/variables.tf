variable "name" {
  description = "Nome da tabela"
  type        = string
}

variable "day_index_name" {
  description = "Nome do índice por dia (partição createdDay, ordenação createdAt)"
  type        = string
  default     = "por-dia"
}

variable "read_capacity" {
  description = "RCU da tabela e do índice (cada um); o sempre gratuito cobre 25 no total"
  type        = number
  default     = 5
}

variable "write_capacity" {
  description = "WCU da tabela e do índice (cada um); o sempre gratuito cobre 25 no total"
  type        = number
  default     = 5
}
