data "aws_caller_identity" "current" {}

locals {
  # OIDC issuer URL for THIS cluster uses the SHARED bucket, per-cluster sub-path
  oidc_issuer_url                  = "https://${var.shared_oidc_bucket}.s3.${var.aws_region}.amazonaws.com/${var.infra_id}"
  oidc_provider_url_without_schema = replace("https://${var.shared_oidc_bucket}.s3.${var.aws_region}.amazonaws.com/${var.infra_id}", "https://", "")
}

# Fetch S3 endpoint SSL certificate details for the OIDC provider thumbprint.
# The TLS cert is for the regional S3 endpoint (e.g. s3.ap-southeast-1.amazonaws.com).
data "tls_certificate" "oidc" {
  url = "https://s3.${var.aws_region}.amazonaws.com"
}

# IAM OIDC Identity Provider for THIS cluster
resource "aws_iam_openid_connect_provider" "oidc" {
  url             = "https://${var.shared_oidc_bucket}.s3.${var.aws_region}.amazonaws.com/${var.infra_id}"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.oidc.certificates[0].sha1_fingerprint]
}

# Common assume role policies using OIDC federation
data "aws_iam_policy_document" "oidc_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.oidc.arn]
    }

    condition {
      test     = "StringLike"
      variable = "${local.oidc_provider_url_without_schema}:sub"
      values   = ["system:serviceaccount:*:*"]
    }
  }
}

# Node pool role also needs EC2 service trust to support instance profile execution
data "aws_iam_policy_document" "node_pool_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.oidc.arn]
    }

    condition {
      test     = "StringLike"
      variable = "${local.oidc_provider_url_without_schema}:sub"
      values   = ["system:serviceaccount:*:*"]
    }
  }

  statement {
    actions = ["sts:AssumeRole"]
    effect  = "Allow"

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

# 1. Control Plane Operator Role
resource "aws_iam_role" "control_plane_operator" {
  name               = "${var.infra_id}-control-plane-operator"
  assume_role_policy = data.aws_iam_policy_document.oidc_assume_role_policy.json
  tags               = var.tags
}

resource "aws_iam_role_policy_attachment" "cpo_ec2" {
  role       = aws_iam_role.control_plane_operator.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
}

resource "aws_iam_role_policy_attachment" "cpo_route53" {
  role       = aws_iam_role.control_plane_operator.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonRoute53FullAccess"
}

resource "aws_iam_role_policy_attachment" "cpo_s3" {
  role       = aws_iam_role.control_plane_operator.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# 2. Image Registry Operator Role
resource "aws_iam_role" "image_registry" {
  name               = "${var.infra_id}-openshift-image-registry"
  assume_role_policy = data.aws_iam_policy_document.oidc_assume_role_policy.json
  tags               = var.tags
}

resource "aws_iam_role_policy_attachment" "registry_s3" {
  role       = aws_iam_role.image_registry.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# 3. Ingress Operator Role
resource "aws_iam_role" "ingress" {
  name               = "${var.infra_id}-openshift-ingress"
  assume_role_policy = data.aws_iam_policy_document.oidc_assume_role_policy.json
  tags               = var.tags
}

resource "aws_iam_role_policy_attachment" "ingress_elb" {
  role       = aws_iam_role.ingress.name
  policy_arn = "arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess"
}

resource "aws_iam_role_policy_attachment" "ingress_route53" {
  role       = aws_iam_role.ingress.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonRoute53FullAccess"
}

# 4. Cloud Controller Role
resource "aws_iam_role" "cloud_controller" {
  name               = "${var.infra_id}-cloud-controller"
  assume_role_policy = data.aws_iam_policy_document.oidc_assume_role_policy.json
  tags               = var.tags
}

resource "aws_iam_role_policy_attachment" "cc_ec2" {
  role       = aws_iam_role.cloud_controller.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
}

resource "aws_iam_role_policy_attachment" "cc_elb" {
  role       = aws_iam_role.cloud_controller.name
  policy_arn = "arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess"
}

# 5. Cloud Network Config Controller Role
resource "aws_iam_role" "cloud_network_config_controller" {
  name               = "${var.infra_id}-cloud-network-config-controller"
  assume_role_policy = data.aws_iam_policy_document.oidc_assume_role_policy.json
  tags               = var.tags
}

resource "aws_iam_role_policy_attachment" "cncc_ec2" {
  role       = aws_iam_role.cloud_network_config_controller.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
}

# 6. EBS CSI Driver Role
resource "aws_iam_role" "aws_ebs_csi_driver_controller" {
  name               = "${var.infra_id}-aws-ebs-csi-driver-controller"
  assume_role_policy = data.aws_iam_policy_document.oidc_assume_role_policy.json
  tags               = var.tags
}

resource "aws_iam_role_policy_attachment" "ebs_csi" {
  role       = aws_iam_role.aws_ebs_csi_driver_controller.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
}

# 7. Node Pool (Worker) Role
resource "aws_iam_role" "node_pool" {
  name               = "${var.infra_id}-node-pool"
  assume_role_policy = data.aws_iam_policy_document.node_pool_assume_role_policy.json
  tags               = var.tags
}

resource "aws_iam_role_policy_attachment" "node_pool_ec2" {
  role       = aws_iam_role.node_pool.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
}

# Instance Profile for EC2 Worker Nodes
resource "aws_iam_instance_profile" "worker" {
  name = "${var.infra_id}-worker"
  role = aws_iam_role.node_pool.name
  tags = var.tags
}

# 8. HCP CLI Role (Required for HCP command execution)
data "aws_iam_policy_document" "hcp_cli_trust_policy" {
  statement {
    actions = ["sts:AssumeRole"]
    effect  = "Allow"
    principals {
      type        = "AWS"
      identifiers = [var.hcp_cli_trust_arn == "" ? "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root" : var.hcp_cli_trust_arn]
    }
  }
}

resource "aws_iam_role" "hcp_cli" {
  name               = "${var.infra_id}-hcp-cli-role"
  assume_role_policy = data.aws_iam_policy_document.hcp_cli_trust_policy.json
  tags               = var.tags
}

resource "aws_iam_role_policy" "hcp_cli" {
  name = "${var.infra_id}-policy"
  role = aws_iam_role.hcp_cli.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "EC2"
        Effect = "Allow"
        Action = [
          "ec2:CreateDhcpOptions", "ec2:DeleteSubnet", "ec2:ReplaceRouteTableAssociation",
          "ec2:DescribeAddresses", "ec2:DescribeInstances", "ec2:DeleteVpcEndpoints",
          "ec2:CreateNatGateway", "ec2:CreateVpc", "ec2:DescribeDhcpOptions",
          "ec2:AttachInternetGateway", "ec2:DeleteVpcEndpointServiceConfigurations",
          "ec2:DeleteRouteTable", "ec2:AssociateRouteTable", "ec2:DescribeInternetGateways",
          "ec2:DescribeAvailabilityZones", "ec2:CreateRoute", "ec2:CreateInternetGateway",
          "ec2:RevokeSecurityGroupEgress", "ec2:ModifyVpcAttribute", "ec2:DeleteInternetGateway",
          "ec2:DescribeVpcEndpointConnections", "ec2:RejectVpcEndpointConnections",
          "ec2:DescribeRouteTables", "ec2:ReleaseAddress", "ec2:AssociateDhcpOptions",
          "ec2:TerminateInstances", "ec2:CreateTags", "ec2:DeleteRoute", "ec2:CreateRouteTable",
          "ec2:DetachInternetGateway", "ec2:DescribeVpcEndpointServiceConfigurations",
          "ec2:DescribeNatGateways", "ec2:DisassociateRouteTable", "ec2:AllocateAddress",
          "ec2:DescribeSecurityGroups", "ec2:RevokeSecurityGroupIngress", "ec2:CreateVpcEndpoint",
          "ec2:DescribeVpcs", "ec2:DeleteSecurityGroup", "ec2:DeleteDhcpOptions",
          "ec2:DeleteNatGateway", "ec2:DescribeVpcEndpoints", "ec2:DeleteVpc",
          "ec2:CreateSubnet", "ec2:DescribeSubnets",
        ]
        Resource = "*"
      },
      {
        Sid    = "ELB"
        Effect = "Allow"
        Action = [
          "elasticloadbalancing:DeleteLoadBalancer", "elasticloadbalancing:DescribeLoadBalancers",
          "elasticloadbalancing:DescribeTargetGroups", "elasticloadbalancing:DeleteTargetGroup",
        ]
        Resource = "*"
      },
      {
        Sid      = "IAMPassRole"
        Effect   = "Allow"
        Action   = "iam:PassRole"
        Resource = ["arn:aws:iam::*:role/*-worker", "arn:aws:iam::*:role/*-node-pool"]
        Condition = {
          "ForAnyValue:StringEqualsIfExists" = { "iam:PassedToService" = "ec2.amazonaws.com" }
        }
      },
      {
        Sid    = "IAM"
        Effect = "Allow"
        Action = [
          "iam:CreateInstanceProfile", "iam:DeleteInstanceProfile", "iam:GetRole",
          "iam:UpdateAssumeRolePolicy", "iam:GetInstanceProfile", "iam:TagRole",
          "iam:RemoveRoleFromInstanceProfile", "iam:CreateRole", "iam:DeleteRole",
          "iam:PutRolePolicy", "iam:AddRoleToInstanceProfile",
          "iam:CreateOpenIDConnectProvider", "iam:ListOpenIDConnectProviders",
          "iam:DeleteRolePolicy", "iam:UpdateRole", "iam:DeleteOpenIDConnectProvider",
          "iam:GetRolePolicy",
        ]
        Resource = "*"
      },
      {
        Sid    = "Route53"
        Effect = "Allow"
        Action = [
          "route53:ListHostedZonesByVPC", "route53:CreateHostedZone", "route53:ListHostedZones",
          "route53:ChangeResourceRecordSets", "route53:ListResourceRecordSets",
          "route53:DeleteHostedZone", "route53:AssociateVPCWithHostedZone", "route53:ListHostedZonesByName",
        ]
        Resource = "*"
      },
      {
        Sid    = "S3"
        Effect = "Allow"
        Action = [
          "s3:ListAllMyBuckets", "s3:ListBucket", "s3:DeleteObject", "s3:DeleteBucket",
        ]
        Resource = "*"
      },
    ]
  })
}
