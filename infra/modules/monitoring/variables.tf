variable "dashboard_name" {
  description = "Nome do dashboard do CloudWatch"
  type        = string
}

variable "query_folder" {
  description = "Pasta das consultas salvas do Logs Insights"
  type        = string
}

variable "function_names" {
  description = "Funções Lambda mostradas no dashboard"
  type        = list(string)
}

variable "log_group_names" {
  description = "Grupos de logs das funções (usados nas consultas salvas e no quadro de erros)"
  type        = list(string)
}

variable "queue_name" {
  description = "Fila de feedbacks críticos"
  type        = string
}

variable "dlq_name" {
  description = "DLQ da fila de críticos"
  type        = string
}

variable "table_name" {
  description = "Tabela de feedbacks"
  type        = string
}

variable "day_index_name" {
  description = "Índice por dia da tabela (lido pelo relatório)"
  type        = string
}

variable "topic_names" {
  description = "Tópicos SNS de e-mail"
  type        = list(string)
}

variable "alarm_topic_arn" {
  description = "Tópico SNS que recebe os alarmes (sem a chave aws/sns: o CloudWatch não publica com ela)"
  type        = string
}

variable "throttle_function_name" {
  description = "Função com alarme de throttles (a de URL pública)"
  type        = string
}
