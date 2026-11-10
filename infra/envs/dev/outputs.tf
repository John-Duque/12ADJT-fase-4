output "region" {
  description = "Região onde os recursos são criados"
  value       = data.aws_region.current.region
}

output "feedback_table" {
  description = "Nome da tabela de feedbacks"
  value       = module.feedback_table.name
}
