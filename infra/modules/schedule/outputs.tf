output "name" {
  description = "Nome da agenda"
  value       = aws_scheduler_schedule.this.name
}

output "arn" {
  description = "ARN da agenda"
  value       = aws_scheduler_schedule.this.arn
}
