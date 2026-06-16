variable "vpc_name" {
  description = "The name of the VPC."
  type        = string
}

variable "vpc_cidr" {
  description = "The CIDR block for the VPC."
  type        = string
}

variable "public_subnets" {
  description = "Definitions for the public subnets. availability_zone is optional and auto-filled when omitted."
  type = list(object({
    cidr              = string
    availability_zone = optional(string)
    name_suffix       = optional(string)
  }))
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway for private subnets (not used by HCP but kept for VPC module symmetry)."
  type        = bool
  default     = false
}

variable "region" {
  description = "The AWS region to create resources in."
  type        = string
}

variable "tags" {
  description = "Additional tags to apply to resources."
  type        = map(string)
  default     = {}
}
