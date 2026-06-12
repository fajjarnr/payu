variable "infra_id" {
  type        = string
  default     = "payu-dev"
  description = "Infrastructure ID used for resource prefixing and S3 bucket naming"
}

variable "aws_region" {
  type        = string
  default     = "us-east-1"
  description = "AWS Region where resources will be provisioned"
}

variable "tags" {
  type        = map(string)
  description = "Common tags to apply to all resources"
  default = {
    "app.kubernetes.io/part-of" = "payu"
    "environment"               = "dev"
  }
}

variable "hcp_cli_trust_arn" {
  type        = string
  default     = ""
  description = "Optional IAM user/role ARN to trust for assuming the HCP CLI role. If empty, defaults to the AWS account root."
}
