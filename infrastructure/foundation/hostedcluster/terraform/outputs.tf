output "shared_oidc_bucket" {
  value       = aws_s3_bucket.shared_oidc.id
  description = "The name of the shared OIDC S3 bucket (used by ALL hosted clusters, with per-cluster sub-paths)"
}

output "cluster_ids" {
  value       = { for k, v in module.iam : k => v.iam_role_arns }
  description = "Per-cluster IAM role ARNs (controlPlaneOperator, imageRegistry, ingress, kubeCloudController, network, nodePool, storage)"
}

output "cluster_vpcs" {
  value       = { for k, v in module.vpc : k => v.vpc_id }
  description = "Per-cluster dedicated VPC IDs"
}

output "cluster_public_subnet_ids" {
  value       = { for k, v in module.vpc : k => v.public_subnet_ids }
  description = "Per-cluster public subnet IDs (ordered by AZ)"
}

output "cluster_worker_security_group_ids" {
  value       = { for k, v in module.vpc : k => v.worker_security_group_id }
  description = "Per-cluster worker security group IDs"
}

output "cluster_oidc_issuer_urls" {
  value       = { for k, v in module.iam : k => v.oidc_issuer_url }
  description = "Per-cluster OIDC issuer URLs"
}

output "cluster_worker_instance_profiles" {
  value       = { for k, v in module.iam : k => v.worker_instance_profile_name }
  description = "Per-cluster worker IAM instance profile names"
}

output "cluster_hcp_cli_role_arns" {
  value       = { for k, v in module.iam : k => v.hcp_cli_role_arn }
  description = "Per-cluster HCP CLI role ARNs"
}
