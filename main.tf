provider "aws" {
  access_key                  = "test"
  secret_key                  = "test"
  region                      = "us-east-1"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true
  s3_use_path_style           = true

  endpoints {
    s3          = "http://localhost:4566"
    ses         = "http://localhost:4566"
    eventbridge = "http://localhost:4566"
    sqs         = "http://localhost:4566"
  }
}

resource "aws_s3_bucket" "algamoney" {
  bucket = "algamoney-api"
}

resource "aws_ses_email_identity" "remetente" {
  email = "testes.algaworks@gmail.com"
}

resource "aws_cloudwatch_event_bus" "algamoney_bus" {
  name = "algamoney-events"
}

resource "aws_sqs_queue" "recurso_criado_queue" {
  name = "recurso-criado-queue"
}

resource "aws_cloudwatch_event_rule" "recurso_criado_rule" {
  name          = "recurso-criado-rule"
  event_bus_name = aws_cloudwatch_event_bus.algamoney_bus.name
  event_pattern = jsonencode({
    "source" : ["algamoney-api"],
    "detail-type" : ["RecursoCriado"]
  })
}

resource "aws_cloudwatch_event_target" "recurso_criado_target" {
  rule          = aws_cloudwatch_event_rule.recurso_criado_rule.name
  event_bus_name = aws_cloudwatch_event_bus.algamoney_bus.name
  target_id     = "sqs-target"
  arn           = aws_sqs_queue.recurso_criado_queue.arn
}

data "aws_iam_policy_document" "sqs_policy_doc" {
  statement {
    effect  = "Allow"
    actions = ["sqs:SendMessage"]
    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }
    resources = [aws_sqs_queue.recurso_criado_queue.arn]
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_cloudwatch_event_rule.recurso_criado_rule.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "recurso_criado_queue_policy" {
  queue_url = aws_sqs_queue.recurso_criado_queue.id
  policy    = data.aws_iam_policy_document.sqs_policy_doc.json
}
