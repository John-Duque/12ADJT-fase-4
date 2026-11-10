output "region" {
  description = "Região onde os recursos são criados"
  value       = data.aws_region.current.region
}
