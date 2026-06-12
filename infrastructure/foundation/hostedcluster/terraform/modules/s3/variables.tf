variable "infra_id" {
  type        = string
  description = "Infrastructure ID used for resource prefixing and S3 bucket naming"
}

variable "tags" {
  type        = map(string)
  description = "Common tags to apply to all resources"
}
