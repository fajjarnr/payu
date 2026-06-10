#!/bin/bash
set -euo pipefail

# PayU HCP - Create IAM Roles for HostedCluster
# Usage: ./create-iam-roles.sh [INFRA_ID] [ACCOUNT_ID]
# Default: INFRA_ID=payu-dev, ACCOUNT_ID from current AWS identity

INFRA_ID="${1:-payu-dev}"
ACCOUNT_ID="${2:-$(aws sts get-caller-identity --query Account --output text)}"
TRUST_POLICY_FILE="/tmp/${INFRA_ID}-trust-policy.json"

echo "Creating IAM roles for infra: ${INFRA_ID} (account: ${ACCOUNT_ID})"

# Trust policy: allows OpenShift OIDC provider to assume roles
cat > "${TRUST_POLICY_FILE}" <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/oidc-storage-${INFRA_ID}-${ACCOUNT_ID}.s3.${AWS_REGION:-us-east-1}.amazonaws.com/${INFRA_ID}"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringLike": {
          "oidc-storage-${INFRA_ID}-${ACCOUNT_ID}.s3.${AWS_REGION:-us-east-1}.amazonaws.com/${INFRA_ID}:sub": "system:serviceaccount:*:*"
        }
      }
    }
  ]
}
EOF

ROLES=(
  "${INFRA_ID}-control-plane-operator"
  "${INFRA_ID}-openshift-image-registry"
  "${INFRA_ID}-openshift-ingress"
  "${INFRA_ID}-cloud-controller"
  "${INFRA_ID}-cloud-network-config-controller"
  "${INFRA_ID}-node-pool"
  "${INFRA_ID}-aws-ebs-csi-driver-controller"
)

for ROLE in "${ROLES[@]}"; do
  echo "Reconciling role: ${ROLE}"
  aws iam create-role \
    --role-name "${ROLE}" \
    --assume-role-policy-document "file://${TRUST_POLICY_FILE}" \
    --tags "Key=app.kubernetes.io/part-of,Value=payu" "Key=environment,Value=dev" \
    2>/dev/null || true
  aws iam update-assume-role-policy \
    --role-name "${ROLE}" \
    --policy-document "file://${TRUST_POLICY_FILE}"
done

# Attach policies for specific roles
echo ""
echo "Attaching policies..."

# Node pool needs EC2 permissions
aws iam attach-role-policy \
  --role-name "${INFRA_ID}-node-pool" \
  --policy-arn "arn:aws:iam::aws:policy/AmazonEC2FullAccess" 2>/dev/null || true

# Image registry needs S3 permissions
aws iam attach-role-policy \
  --role-name "${INFRA_ID}-openshift-image-registry" \
  --policy-arn "arn:aws:iam::aws:policy/AmazonS3FullAccess" 2>/dev/null || true

# Cloud controller needs EC2/ELB permissions
aws iam attach-role-policy \
  --role-name "${INFRA_ID}-cloud-controller" \
  --policy-arn "arn:aws:iam::aws:policy/AmazonEC2FullAccess" 2>/dev/null || true
aws iam attach-role-policy \
  --role-name "${INFRA_ID}-cloud-controller" \
  --policy-arn "arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess" 2>/dev/null || true

# EBS CSI needs EC2 permissions
aws iam attach-role-policy \
  --role-name "${INFRA_ID}-aws-ebs-csi-driver-controller" \
  --policy-arn "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy" 2>/dev/null || true

# Control Plane Operator needs EC2, Route53, and S3 permissions
aws iam attach-role-policy --role-name "${INFRA_ID}-control-plane-operator" --policy-arn "arn:aws:iam::aws:policy/AmazonEC2FullAccess" 2>/dev/null || true
aws iam attach-role-policy --role-name "${INFRA_ID}-control-plane-operator" --policy-arn "arn:aws:iam::aws:policy/AmazonRoute53FullAccess" 2>/dev/null || true
aws iam attach-role-policy --role-name "${INFRA_ID}-control-plane-operator" --policy-arn "arn:aws:iam::aws:policy/AmazonS3FullAccess" 2>/dev/null || true

# Ingress needs ELB and Route53 permissions
aws iam attach-role-policy --role-name "${INFRA_ID}-openshift-ingress" --policy-arn "arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess" 2>/dev/null || true
aws iam attach-role-policy --role-name "${INFRA_ID}-openshift-ingress" --policy-arn "arn:aws:iam::aws:policy/AmazonRoute53FullAccess" 2>/dev/null || true

# Cloud Network Config Controller needs EC2 permissions
aws iam attach-role-policy --role-name "${INFRA_ID}-cloud-network-config-controller" --policy-arn "arn:aws:iam::aws:policy/AmazonEC2FullAccess" 2>/dev/null || true

echo ""
echo "Done. Verify with:"
echo "  aws iam list-roles --query 'Roles[?starts_with(RoleName,\`${INFRA_ID}\`)].RoleName' --output table"

rm -f "${TRUST_POLICY_FILE}"
