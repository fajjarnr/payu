output "oidc_bucket_name" {
  value       = module.s3.bucket_name
  description = "The name of the OIDC storage S3 bucket"
}

output "oidc_bucket_arn" {
  value       = module.s3.bucket_arn
  description = "The ARN of the OIDC storage S3 bucket"
}

output "oidc_issuer_url" {
  value       = module.iam.oidc_issuer_url
  description = "The OIDC issuer URL for the Hosted Cluster"
}

output "oidc_provider_arn" {
  value       = module.iam.oidc_provider_arn
  description = "The ARN of the IAM OIDC Connect Provider"
}

output "worker_instance_profile_name" {
  value       = module.iam.worker_instance_profile_name
  description = "The name of the IAM instance profile for worker nodes"
}

output "hcp_cli_role_arn" {
  value       = module.iam.hcp_cli_role_arn
  description = "The ARN of the HCP CLI Role"
}

output "iam_role_arns" {
  value       = module.iam.iam_role_arns
  description = "A map of IAM role ARNs for Hosted Cluster components"
}
