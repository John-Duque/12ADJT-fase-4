output "region" {
  description = "Região onde os recursos são criados"
  value       = data.aws_region.current.region
}

output "feedback_table" {
  description = "Nome da tabela de feedbacks"
  value       = module.feedback_table.name
}

output "critical_queue_url" {
  description = "URL da fila de feedbacks críticos"
  value       = module.critical_queue.url
}

output "critical_dlq_url" {
  description = "URL da DLQ da fila de críticos"
  value       = module.critical_queue.dlq_url
}

output "critical_topic_arn" {
  description = "Tópico do e-mail de urgência"
  value       = module.critical_topic.arn
}

output "report_topic_arn" {
  description = "Tópico do relatório semanal"
  value       = module.report_topic.arn
}

output "ingestion_url" {
  description = "Function URL da ingestão (POST <url>avaliacao)"
  value       = module.ingestion_function.url
}

output "function_names" {
  description = "Nomes das funções Lambda"
  value = {
    ingestion = module.ingestion_function.name
    notifier  = module.notifier_function.name
    report    = module.report_function.name
  }
}

output "report_schedule" {
  description = "Agenda do relatório semanal"
  value       = module.report_schedule.name
}
