variable "infra_id" {
  type        = string
  description = "Infrastructure ID used for resource prefixing"
}

variable "aws_region" {
  type        = string
  description = "AWS Region where resources are located"
}

variable "tags" {
  type        = map(string)
  description = "Common tags to apply to all resources"
}

variable "hcp_cli_trust_arn" {
  type        = string
  description = "Optional IAM user/role ARN to trust for assuming the HCP CLI role"
}

variable "oidc_bucket_name" {
  type        = string
  description = "The name of the OIDC storage S3 bucket"
}

variable "oidc_bucket_arn" {
  type        = string
  description = "The ARN of the OIDC storage S3 bucket"
}

variable "oidc_bucket_domain" {
  type        = string
  description = "The regional domain name of the OIDC storage S3 bucket"
}
