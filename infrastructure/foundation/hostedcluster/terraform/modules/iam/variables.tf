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

variable "shared_oidc_bucket" {
  type        = string
  description = "Name of the SHARED OIDC S3 bucket used by ALL hosted clusters. The per-cluster OIDC issuer URL is constructed as https://<bucket>.s3.<region>.amazonaws.com/<infra_id>"
}
