variable "name" {
  description = "Nome da agenda"
  type        = string
}

variable "description" {
  description = "Descrição da agenda"
  type        = string
}

variable "role_name" {
  description = "Nome do papel IAM da agenda; precisa começar com feedback-"
  type        = string

  validation {
    condition     = startswith(var.role_name, "feedback-")
    error_message = "O papel precisa começar com feedback- (ADR 0004)."
  }
}

variable "schedule_expression" {
  description = "Expressão cron ou rate do EventBridge Scheduler"
  type        = string
}

variable "timezone" {
  description = "Fuso da expressão cron"
  type        = string
}

variable "target_arn" {
  description = "ARN da Lambda (ou do alias) chamada pela agenda"
  type        = string
}

variable "input" {
  description = "JSON enviado para a função"
  type        = string
  default     = "{}"
}

variable "maximum_retry_attempts" {
  description = "Tentativas extras se a invocação falhar"
  type        = number
  default     = 2
}
