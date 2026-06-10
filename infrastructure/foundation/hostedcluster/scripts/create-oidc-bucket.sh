#!/bin/bash
set -euo pipefail

# PayU HCP - Create S3 OIDC bucket for HostedCluster
# Usage: ./create-oidc-bucket.sh [INFRA_ID] [REGION]

INFRA_ID="${1:-payu-dev}"
REGION="${2:-us-east-1}"
BUCKET="oidc-storage-${INFRA_ID}"

echo "Creating S3 OIDC bucket: ${BUCKET} in ${REGION}"

# Create bucket
if [ "${REGION}" = "us-east-1" ]; then
  aws s3api create-bucket --bucket "${BUCKET}"
else
  aws s3api create-bucket --bucket "${BUCKET}" \
    --create-bucket-configuration "LocationConstraint=${REGION}"
fi

# Remove public access block
aws s3api delete-public-access-block --bucket "${BUCKET}"

# Set bucket policy for public read
cat > /tmp/${BUCKET}-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${BUCKET}/*"
    }
  ]
}
EOF

aws s3api put-bucket-policy --bucket "${BUCKET}" \
  --policy "file:///tmp/${BUCKET}-policy.json"

rm -f "/tmp/${BUCKET}-policy.json"

echo ""
echo "Done. Bucket ${BUCKET} created with public read access."
echo ""
echo "Update issuerURL in hostedcluster-payu.yaml to:"
echo "  https://${BUCKET}.s3.${REGION}.amazonaws.com/${INFRA_ID}"
