output "dashboard_name" {
  description = "Nome do dashboard"
  value       = aws_cloudwatch_dashboard.this.dashboard_name
}

output "dashboard_url" {
  description = "Endereço do dashboard no console"
  value       = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#dashboards/dashboard/${aws_cloudwatch_dashboard.this.dashboard_name}"
}

output "query_names" {
  description = "Consultas salvas do Logs Insights"
  value       = [for query in aws_cloudwatch_query_definition.this : query.name]
}

output "alarm_names" {
  description = "Alarmes que avisam os administradores"
  value       = concat([for alarm in aws_cloudwatch_metric_alarm.errors : alarm.alarm_name], [aws_cloudwatch_metric_alarm.dlq.alarm_name, aws_cloudwatch_metric_alarm.throttles.alarm_name])
}
