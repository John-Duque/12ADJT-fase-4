output "name" {
  description = "Nome da tabela"
  value       = aws_dynamodb_table.this.name
}

output "arn" {
  description = "ARN da tabela"
  value       = aws_dynamodb_table.this.arn
}

output "day_index_name" {
  description = "Nome do índice por dia"
  value       = var.day_index_name
}
