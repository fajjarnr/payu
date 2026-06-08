#!/bin/bash
set -euo pipefail

# PayU HCP - Full prerequisites setup for HostedCluster
# Usage: ./setup-prerequisites.sh [INFRA_ID] [REGION]
# Run this BEFORE applying hostedcluster-payu.yaml

INFRA_ID="${1:-payu-dev}"
REGION="${2:-ap-southeast-1}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET="oidc-storage-${INFRA_ID}"

echo "============================================"
echo "PayU HCP Prerequisites Setup"
echo "InfraID: ${INFRA_ID}"
echo "Region:  ${REGION}"
echo "Account: ${ACCOUNT_ID}"
echo "============================================"

# ── Step 1: Create OIDC S3 Bucket ─────────────────────
echo ""
echo "[1/4] Creating OIDC S3 bucket: ${BUCKET}"

if [ "${REGION}" = "us-east-1" ]; then
  aws s3api create-bucket --bucket "${BUCKET}" 2>/dev/null || echo "  Bucket already exists"
else
  aws s3api create-bucket --bucket "${BUCKET}" \
    --create-bucket-configuration "LocationConstraint=${REGION}" 2>/dev/null || echo "  Bucket already exists"
fi

aws s3api delete-public-access-block --bucket "${BUCKET}" 2>/dev/null || true

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
echo "  Done."

# ── Step 2: Create OIDC S3 Secret ─────────────────────
echo ""
echo "[2/4] Creating OIDC S3 credentials secret"

oc create secret generic hypershift-operator-oidc-provider-s3-credentials \
  --from-literal=bucket="${BUCKET}" \
  --from-literal=region="${REGION}" \
  --from-file=credentials="${HOME}/.aws/credentials" \
  -n local-cluster 2>/dev/null || echo "  Secret already exists (delete and recreate if needed)"

echo "  Done."

# ── Step 3: Create IAM Roles ──────────────────────────
echo ""
echo "[3/4] Creating IAM roles"

TRUST_POLICY_FILE="/tmp/${INFRA_ID}-trust-policy.json"
cat > "${TRUST_POLICY_FILE}" <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/${BUCKET}.s3.${REGION}.amazonaws.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "${BUCKET}.s3.${REGION}.amazonaws.com/${INFRA_ID}:sub": "system:serviceaccount:openshift-cluster-api:capa-controller-manager"
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
  aws iam create-role \
    --role-name "${ROLE}" \
    --assume-role-policy-document "file://${TRUST_POLICY_FILE}" \
    --tags "Key=app.kubernetes.io/part-of,Value=payu" "Key=environment,Value=dev" \
    2>/dev/null && echo "  Created: ${ROLE}" || echo "  Exists:  ${ROLE}"
done

# Attach policies
aws iam attach-role-policy --role-name "${INFRA_ID}-node-pool" --policy-arn "arn:aws:iam::aws:policy/AmazonEC2FullAccess" 2>/dev/null || true
aws iam attach-role-policy --role-name "${INFRA_ID}-openshift-image-registry" --policy-arn "arn:aws:iam::aws:policy/AmazonS3FullAccess" 2>/dev/null || true
aws iam attach-role-policy --role-name "${INFRA_ID}-cloud-controller" --policy-arn "arn:aws:iam::aws:policy/AmazonEC2FullAccess" 2>/dev/null || true
aws iam attach-role-policy --role-name "${INFRA_ID}-cloud-controller" --policy-arn "arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess" 2>/dev/null || true
aws iam attach-role-policy --role-name "${INFRA_ID}-aws-ebs-csi-driver-controller" --policy-arn "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy" 2>/dev/null || true

rm -f "${TRUST_POLICY_FILE}"
echo "  Done."

# ── Step 4: Create Secrets ────────────────────────────
echo ""
echo "[4/4] Creating secrets in local-cluster namespace"

# Pull secret
oc get secret development-pull-secret -n local-cluster -o jsonpath='{.data.\.dockerconfigjson}' | base64 -d | \
  oc create secret generic ${INFRA_ID}-pull-secret -n local-cluster --from-file=.dockerconfigjson=/dev/stdin --type=kubernetes.io/dockerconfigjson 2>/dev/null || echo "  Pull secret already exists"

# Etcd encryption key (32 raw bytes for AES-256)
openssl rand 32 | oc create secret generic ${INFRA_ID}-etcd-encryption-key -n local-cluster --from-file=key=/dev/stdin 2>/dev/null || echo "  Etcd key already exists"

echo "  Done."

echo ""
echo "============================================"
echo "Setup complete! Now apply the HostedCluster:"
echo ""
echo "  oc apply -f hostedcluster-payu.yaml"
echo "  oc apply -f nodepools-payu.yaml"
echo ""
echo "IssuerURL should be:"
echo "  https://${BUCKET}.s3.${REGION}.amazonaws.com/${INFRA_ID}"
echo "============================================"
