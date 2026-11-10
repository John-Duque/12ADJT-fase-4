variable "name" {
  description = "Nome da função Lambda"
  type        = string
}

variable "role_name" {
  description = "Nome do papel IAM; precisa começar com feedback- (o papel de deploy só gerencia esses)"
  type        = string

  validation {
    condition     = startswith(var.role_name, "feedback-")
    error_message = "O papel precisa começar com feedback- (ADR 0004)."
  }
}

variable "description" {
  description = "Descrição da função"
  type        = string
}

variable "handler" {
  description = "Handler do Quarkus (HTTP ou Funqy)"
  type        = string
}

variable "zip_path" {
  description = "Caminho do function.zip gerado por mvn package -Plambda"
  type        = string
}

variable "memory_size" {
  description = "Memória em MB (o sempre gratuito cobre 400 mil GB-segundo por mês)"
  type        = number
  default     = 512
}

variable "timeout" {
  description = "Tempo máximo de execução em segundos"
  type        = number
}

variable "environment" {
  description = "Variáveis de ambiente da função"
  type        = map(string)
  default     = {}
}

variable "policy_json" {
  description = "Permissões da função além dos logs (documento IAM em JSON)"
  type        = string
}

variable "reserved_concurrency" {
  description = "Execuções simultâneas reservadas; -1 deixa sem reserva"
  type        = number
  default     = -1
}

variable "public_url" {
  description = "Cria a Function URL pública (HTTPS, sem autenticação) no alias live"
  type        = bool
  default     = false
}

variable "log_retention_days" {
  description = "Retenção dos logs no CloudWatch"
  type        = number
  default     = 7
}
