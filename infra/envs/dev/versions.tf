terraform {
  required_version = "~> 1.17"

  # State e lock no HCP Terraform; o workspace usa execution mode Local (ADR 0003)
  cloud {
    organization = "john-duque-12adjt"

    workspaces {
      name = "12adjt-fase-4-dev"
    }
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.72"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      projeto        = var.project
      ambiente       = var.environment
      gerenciado-por = "terraform"
    }
  }
}
