output "arn" {
  description = "ARN do tópico"
  value       = aws_sns_topic.this.arn
}

output "name" {
  description = "Nome do tópico"
  value       = aws_sns_topic.this.name
}
