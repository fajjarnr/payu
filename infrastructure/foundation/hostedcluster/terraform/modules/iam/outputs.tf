output "oidc_provider_arn" {
  value       = aws_iam_openid_connect_provider.oidc.arn
  description = "The ARN of the IAM OIDC Connect Provider"
}

output "oidc_issuer_url" {
  value       = aws_iam_openid_connect_provider.oidc.url
  description = "The OIDC issuer URL for the Hosted Cluster"
}

output "worker_instance_profile_name" {
  value       = aws_iam_instance_profile.worker.name
  description = "The name of the IAM instance profile for worker nodes"
}

output "hcp_cli_role_arn" {
  value       = aws_iam_role.hcp_cli.arn
  description = "The ARN of the HCP CLI Role"
}

output "iam_role_arns" {
  value = {
    control_plane_operator         = aws_iam_role.control_plane_operator.arn
    openshift_image_registry       = aws_iam_role.image_registry.arn
    openshift_ingress              = aws_iam_role.ingress.arn
    cloud_controller               = aws_iam_role.cloud_controller.arn
    cloud_network_config_controller = aws_iam_role.cloud_network_config_controller.arn
    aws_ebs_csi_driver_controller  = aws_iam_role.aws_ebs_csi_driver_controller.arn
    node_pool                      = aws_iam_role.node_pool.arn
  }
  description = "A map of IAM role ARNs for Hosted Cluster components"
}
