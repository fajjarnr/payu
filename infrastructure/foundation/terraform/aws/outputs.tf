output "vpc_id" {
  description = "The ID of the VPC."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "The IDs of the public subnets."
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "The IDs of the private subnets."
  value       = module.vpc.private_subnet_ids
}

output "security_group_id" {
  description = "The ID of the security group."
  value       = module.vpc.security_group_id
}

output "public_zone_ids" {
  description = "Map of public Route53 zone name to hosted zone ID."
  value       = { for name, zone in aws_route53_zone.public : name => zone.zone_id }
}

output "public_zone_name_servers" {
  description = "Map of public Route53 zone name to its name servers."
  value       = { for name, zone in aws_route53_zone.public : name => zone.name_servers }
}

output "bastion_instance_id" {
  description = "The ID of the bastion EC2 instance."
  value       = module.bastion.instance_id
}

output "bastion_public_ip" {
  description = "The public IP address of the bastion host."
  value       = module.bastion.instance_public_ip
}

output "bastion_private_ip" {
  description = "The private IP address of the bastion host."
  value       = module.bastion.instance_private_ip
}
