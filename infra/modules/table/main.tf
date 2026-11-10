# Tabela de feedbacks no modo provisionado (sempre gratuito até 25 RCU / 25 WCU somados).
# Criptografia em repouso com a chave da AWS (padrão, sem custo); sem PITR nem backups, que são cobrados.

resource "aws_dynamodb_table" "this" {
  name           = var.name
  billing_mode   = "PROVISIONED"
  read_capacity  = var.read_capacity
  write_capacity = var.write_capacity
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "createdDay"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  # O relatório semanal consulta um dia por vez, já ordenado pela data de envio
  global_secondary_index {
    name            = var.day_index_name
    projection_type = "ALL"
    read_capacity   = var.read_capacity
    write_capacity  = var.write_capacity

    key_schema {
      attribute_name = "createdDay"
      key_type       = "HASH"
    }

    key_schema {
      attribute_name = "createdAt"
      key_type       = "RANGE"
    }
  }

  point_in_time_recovery {
    enabled = false
  }
}
