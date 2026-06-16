variable "region" {
  type        = string
  default     = "ap-southeast-1"
  description = "AWS Region where all resources will be provisioned"
}

variable "hcp_cli_trust_arn" {
  type        = string
  default     = ""
  description = "Optional IAM user/role ARN to trust for assuming the HCP CLI role. If empty, defaults to the AWS account root."
}

variable "common_tags" {
  type        = map(string)
  description = "Common tags applied to all resources across all clusters"
  default = {
    "app.kubernetes.io/part-of" = "payu"
    "cost-center"               = "platform-engineering"
    "owner"                     = "payu-team"
    "ManagedBy"                 = "terraform"
  }
}

# Single shared OIDC bucket used by ALL clusters (per-cluster sub-paths).
# HCP operator publishes OIDC docs at s3://<bucket>/<cluster>/.well-known/*
# The HCP operator secret points to this one bucket; the per-cluster IAM OIDC
# providers reference the per-cluster sub-paths.
variable "shared_oidc_bucket" {
  type        = string
  default     = "oidc-storage-payu-shared"
  description = "Name (without account suffix) of the S3 bucket used by ALL hosted clusters for OIDC discovery. The bucket is suffixed with the AWS account ID at apply time."
}

# Per-cluster configuration map
variable "clusters" {
  type = map(object({
    name            = string
    infra_id        = string
    ocp_version     = string
    base_domain     = string
    private_zone_id = string
    public_zone_id  = string
    vpc_cidr        = string
    public_subnets = list(object({
      cidr              = string
      availability_zone = optional(string)
    }))
    cluster_network_cidr  = string
    service_network_cidr  = string
    node_pool_replicas    = number
    node_instance_type    = string
    node_root_volume_size = number
    environment           = string
    extra_tags            = map(string)
  }))
  description = "Map of HostedCluster definitions keyed by cluster name. Each entry provisions a dedicated VPC, per-cluster IAM roles, and shares one OIDC S3 bucket."
  default     = {}
}
