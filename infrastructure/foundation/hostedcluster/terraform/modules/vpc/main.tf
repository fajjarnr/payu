data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  base_tags = var.tags
  az_names  = data.aws_availability_zones.available.names

  public_subnets = {
    for idx, subnet in var.public_subnets :
    format("%03d", idx) => merge(
      subnet,
      {
        availability_zone = coalesce(try(subnet.availability_zone, null), element(local.az_names, idx))
        name_suffix       = try(subnet.name_suffix, null)
      }
    )
  }
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  instance_tenancy     = "default"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(local.base_tags, { Name = var.vpc_name })
}

resource "aws_subnet" "public" {
  for_each                                    = local.public_subnets
  vpc_id                                      = aws_vpc.main.id
  cidr_block                                  = each.value.cidr
  availability_zone                           = each.value.availability_zone
  map_public_ip_on_launch                     = true
  enable_resource_name_dns_a_record_on_launch = true

  tags = merge(
    local.base_tags,
    {
      Name                                    = "${var.vpc_name}-public-${coalesce(each.value.name_suffix, each.value.availability_zone)}"
      "kubernetes.io/cluster/${var.vpc_name}" = "shared"
      "kubernetes.io/role/elb"                = "1"
    }
  )
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(local.base_tags, { Name = "${var.vpc_name}-igw" })
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(local.base_tags, { Name = "${var.vpc_name}-public-rt" })
}

resource "aws_route_table_association" "public" {
  for_each       = aws_subnet.public
  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "worker" {
  name        = "${var.vpc_name}-worker-sg"
  description = "Allow all inbound/outbound traffic for HostedCluster worker nodes (NLB preserves source IP)"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [var.vpc_cidr, "0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.base_tags, { Name = "${var.vpc_name}-worker-sg" })
}
