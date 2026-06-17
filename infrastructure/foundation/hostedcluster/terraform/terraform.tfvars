# Terraform variables for HostedCluster infrastructure
# This file provisions AWS infrastructure (VPC + S3 + IAM) for 2 HostedClusters
# on top of the payu-8tmf2 management cluster.

region = "ap-southeast-1"

# Optional: limit which IAM principal can assume the HCP CLI role.
# Empty = AWS account root can assume it.
hcp_cli_trust_arn = ""

common_tags = {
  "app.kubernetes.io/part-of" = "payu"
  "cost-center"               = "platform-engineering"
  "owner"                     = "payu-team"
  "ManagedBy"                 = "terraform"
}

# ──────────────────────────────────────────────────────────────────────────────
# Cluster #1 — payu-onprem (OCP 4.18, on-prem flavor)
# Dedicated VPC 10.200.0.0/16, 1 worker node in ap-southeast-1a
# ──────────────────────────────────────────────────────────────────────────────
clusters = {
  payu-onprem = {
    name            = "payu-onprem"
    infra_id        = "payu-onprem"
    ocp_version     = "4.15.59-multi"
    base_domain     = "payu.ocp.fajjjar.my.id"
    private_zone_id = "Z0688851VIBKG68U8DFU"
    public_zone_id  = "Z0716734HV77ZJQGV03V"
    vpc_cidr        = "10.200.0.0/16"
    public_subnets = [
      {
        cidr              = "10.200.0.0/20"
        availability_zone = "ap-southeast-1a"
      },
    ]
    cluster_network_cidr  = "10.132.0.0/14"
    service_network_cidr  = "172.31.0.0/16"
    node_pool_replicas    = 1
    node_instance_type    = "m6a.2xlarge"
    node_root_volume_size = 120
    environment           = "onprem"
    extra_tags = {
      "channel" = "stable-4.15"
    }
  }

  # ────────────────────────────────────────────────────────────────────────────
  # Cluster #2 — payu-cloud (OCP 4.20, cloud flavor)
  # Dedicated VPC 10.201.0.0/16, 1 worker node in ap-southeast-1a
  # ────────────────────────────────────────────────────────────────────────────
  payu-cloud = {
    name            = "payu-cloud"
    infra_id        = "payu-cloud"
    ocp_version     = "4.20.24-multi"
    base_domain     = "payu.ocp.fajjjar.my.id"
    private_zone_id = "Z0688851VIBKG68U8DFU"
    public_zone_id  = "Z0716734HV77ZJQGV03V"
    vpc_cidr        = "10.201.0.0/16"
    public_subnets = [
      {
        cidr              = "10.201.0.0/20"
        availability_zone = "ap-southeast-1a"
      },
    ]
    cluster_network_cidr  = "10.136.0.0/14"
    service_network_cidr  = "172.32.0.0/16"
    node_pool_replicas    = 1
    node_instance_type    = "m6a.2xlarge"
    node_root_volume_size = 120
    environment           = "cloud"
    extra_tags = {
      "channel" = "stable-4.20"
    }
  }
}
