module "s3" {
  source = "./modules/s3"

  infra_id = var.infra_id
  tags     = var.tags
}

module "iam" {
  source = "./modules/iam"

  infra_id           = var.infra_id
  aws_region         = var.aws_region
  tags               = var.tags
  hcp_cli_trust_arn  = var.hcp_cli_trust_arn
  oidc_bucket_name   = module.s3.bucket_name
  oidc_bucket_arn    = module.s3.bucket_arn
  oidc_bucket_domain = module.s3.bucket_domain
}
