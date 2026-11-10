output "name" {
  description = "Nome da função"
  value       = aws_lambda_function.this.function_name
}

output "alias_arn" {
  description = "ARN do alias live (o que os gatilhos chamam)"
  value       = aws_lambda_alias.live.arn
}

output "version" {
  description = "Versão publicada para onde o alias live aponta"
  value       = aws_lambda_function.this.version
}

output "role_arn" {
  description = "ARN do papel da função"
  value       = aws_iam_role.this.arn
}

output "log_group_name" {
  description = "Grupo de logs da função"
  value       = aws_cloudwatch_log_group.this.name
}

output "url" {
  description = "Function URL (null quando public_url = false)"
  value       = var.public_url ? aws_lambda_function_url.this[0].function_url : null
}
