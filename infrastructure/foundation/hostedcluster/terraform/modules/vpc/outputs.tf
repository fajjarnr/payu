output "vpc_id" {
  description = "The ID of the VPC."
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "The IDs of the public subnets (ordered)."
  value       = [for key in sort(keys(aws_subnet.public)) : aws_subnet.public[key].id]
}

output "worker_security_group_id" {
  description = "The ID of the worker security group."
  value       = aws_security_group.worker.id
}

output "internet_gateway_id" {
  description = "The ID of the internet gateway."
  value       = aws_internet_gateway.main.id
}
