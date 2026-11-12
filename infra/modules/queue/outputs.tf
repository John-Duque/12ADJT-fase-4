output "url" {
  description = "URL da fila principal"
  value       = aws_sqs_queue.this.url
}

output "arn" {
  description = "ARN da fila principal"
  value       = aws_sqs_queue.this.arn
}

output "name" {
  description = "Nome da fila principal"
  value       = aws_sqs_queue.this.name
}

output "dlq_url" {
  description = "URL da DLQ"
  value       = aws_sqs_queue.dlq.url
}

output "dlq_arn" {
  description = "ARN da DLQ"
  value       = aws_sqs_queue.dlq.arn
}

output "dlq_name" {
  description = "Nome da DLQ"
  value       = aws_sqs_queue.dlq.name
}
