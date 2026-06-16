###############################################################################
# Per-cluster infrastructure
#
# Each entry in var.clusters provisions:
#   - A dedicated VPC + public subnets + IGW + worker SG
#   - Per-cluster IAM roles (CPO, registry, ingress, KCC, CNCC, EBS CSI, NP)
#     + 1 HCP CLI role + 1 instance profile
#   - A per-cluster IAM OIDC provider pointing to the SHARED OIDC bucket at
#     the per-cluster sub-path (s3://<shared-bucket>/<cluster>/...)
#
# The SHARED OIDC S3 bucket itself is created once (not per-cluster), and the
# HCP operator's `hypershift-operator-oidc-provider-s3-credentials` secret in
# the `local-cluster` namespace must point to it.
###############################################################################

data "aws_caller_identity" "current" {}

locals {
  clusters                = var.clusters
  shared_oidc_bucket_name = "${var.shared_oidc_bucket}-${data.aws_caller_identity.current.account_id}"
  shared_oidc_bucket_arn  = "arn:aws:s3:::${local.shared_oidc_bucket_name}"
}

# --- Single shared OIDC S3 bucket --------------------------------------------
# All clusters' OIDC discovery docs are published under per-cluster sub-paths:
#   s3://<shared-bucket>/<cluster-name>/.well-known/openid-configuration
#   s3://<shared-bucket>/<cluster-name>/openid/v1/jwks
resource "aws_s3_bucket" "shared_oidc" {
  bucket        = local.shared_oidc_bucket_name
  force_destroy = true

  tags = var.common_tags
}

resource "aws_s3_bucket_ownership_controls" "shared_oidc" {
  bucket = aws_s3_bucket.shared_oidc.id
  rule { object_ownership = "ObjectWriter" }
}

resource "aws_s3_bucket_public_access_block" "shared_oidc" {
  bucket = aws_s3_bucket.shared_oidc.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "shared_oidc" {
  bucket = aws_s3_bucket.shared_oidc.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicRead"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${local.shared_oidc_bucket_arn}/*"
    }]
  })

  depends_on = [aws_s3_bucket_public_access_block.shared_oidc]
}

# --- Per-cluster VPC --------------------------------------------------------
module "vpc" {
  source   = "./modules/vpc"
  for_each = local.clusters

  vpc_name       = "${each.value.infra_id}-vpc"
  vpc_cidr       = each.value.vpc_cidr
  public_subnets = each.value.public_subnets
  region         = var.region
  tags           = merge(var.common_tags, each.value.extra_tags, { environment = each.value.environment })
}

# --- Per-cluster IAM roles (each cluster's roles reference its own OIDC
# provider, but ALL OIDC providers point to the SHARED bucket at the
# per-cluster sub-path) -------------------------------------------------------
module "iam" {
  source   = "./modules/iam"
  for_each = local.clusters

  infra_id           = each.value.infra_id
  aws_region         = var.region
  tags               = merge(var.common_tags, each.value.extra_tags, { environment = each.value.environment })
  hcp_cli_trust_arn  = var.hcp_cli_trust_arn
  shared_oidc_bucket = local.shared_oidc_bucket_name
}
