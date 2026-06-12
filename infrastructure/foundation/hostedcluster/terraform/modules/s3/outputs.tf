output "bucket_name" {
  value       = aws_s3_bucket.oidc.id
  description = "The name of the OIDC storage S3 bucket"
}

output "bucket_arn" {
  value       = aws_s3_bucket.oidc.arn
  description = "The ARN of the OIDC storage S3 bucket"
}

output "bucket_domain" {
  value       = aws_s3_bucket.oidc.bucket_regional_domain_name
  description = "The regional domain name of the OIDC storage S3 bucket"
}
