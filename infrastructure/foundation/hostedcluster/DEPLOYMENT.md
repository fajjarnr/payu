# PayU HCP — `payu-onprem` (4.15) + `payu-cloud` (4.20) Deployment Guide

> **Hosted Clusters**: `payu-onprem` (v4.15.43, 1 node) + `payu-cloud` (v4.20.24, 1 node)
> **Management Cluster**: `payu-8tmf2` (OCP 4.20.24, MCE 2.11.2)
> **Platform**: AWS ap-southeast-1 (dedicated VPC per cluster, shared OIDC bucket)
> **CNI**: OVN-Kubernetes (`networkType: OVNKubernetes` — natively managed, no manual CNI setup required)
> **Last Updated**: 2026-06-24
> **References**: [OCP 4.20 HCP Docs](https://docs.redhat.com/en/documentation/openshift_container_platform/4.20/html/hosted_control_planes/) | [ROSA Best Practices](https://cloud.redhat.com/experts/rosa/best-practices-recommendations/)

---

## 0. Executive Summary

| Item | Value |
|:-----|:------|
| AWS Account | `955370087474` |
| Region | `ap-southeast-1` |
| HCP operator version | `35cddf08d3e492ec2b328a832a60a463407dd556` (MCE 2.11.2) |
| **Workaround for HCP 35cddf08** | `payu-system/hcp-audience-fixer` MutatingWebhook (patches `token-minter` to use `--token-audience=sts.amazonaws.com` with PEM cert CA bundle) |
| Terraform | `infrastructure/foundation/hostedcluster/terraform/` — `for_each` multi-cluster with shared OIDC bucket, dedicated VPCs |
| OIDC bucket | `oidc-storage-payu-shared-955370087474` (shared, per-cluster sub-paths) |
| `payu-onprem` | 4.15.43, 1× m6a.4xlarge, ap-southeast-1a, autoRepair=true |
| `payu-cloud` | 4.20.24, 1× m6a.4xlarge, ap-southeast-1a, autoRepair=true |
| CNI | OVN-Kubernetes (native, automatic node provisioning) |
| Final state | **Both HCPs AVAILABLE, NodePools Ready, nodes join and register automatically** |

---

## 1. Pre-requisites

### 1.1 Management Cluster (ap-southeast-1)

| Account | Region | Activity | Validation | Status |
|:--------|:-------|:---------|:-----------|:-------|
| 955370087474 | ap-southeast-1 | Check MCE Operator | `multicluster-engine.v2.11.2` Succeeded | PASSED |
| 955370087474 | ap-southeast-1 | Check `local-cluster` managedcluster | `AVAILABLE=True` | PASSED |
| 955370087474 | ap-southeast-1 | Check HyperShift Operator | 2 pods `Running` in `hypershift` ns | PASSED |
| 955370087474 | ap-southeast-1 | Check HCP CLI | `hcp version` → `35cddf08...` | PASSED |
| 955370087474 | ap-southeast-1 | Check pull-secret | `oc get secret pull-secret -n openshift-config` exists | PASSED |

### 1.2 Networking & CIDR Allocation

| Network | `payu-onprem` | `payu-cloud` | Overlap |
|:--------|:--------------|:-------------|:--------|
| VPC CIDR | `10.200.0.0/16` (dedicated) | `10.201.0.0/16` (dedicated) | NO |
| clusterNetwork | `10.132.0.0/14` | `10.136.0.0/14` | NO |
| serviceNetwork | `172.31.0.0/16` | `172.32.0.0/16` | NO |
| Public subnet (1a) | `10.200.0.0/20` | `10.201.0.0/20` | NO |
| Base domain | `payu.ocp.fajjjar.my.id` | `payu.ocp.fajjjar.my.id` | shared |
| Private Route53 zone | `Z09069013903ZAKGG8DWP` (`payu.ocp.fajjjar.my.id`) | `Z09069013903ZAKGG8DWP` | shared |
| Public Route53 zone | `Z01586331DWCIX83XX3FH` (`ocp.fajjjar.my.id`) | `Z01586331DWCIX83XX3FH` | shared |

> [!IMPORTANT]
> **Dedicated VPC per cluster** — Each HCP gets its own VPC to ensure full network isolation during migration simulation and prevent subnet tag collisions (`kubernetes.io/cluster/<id>`).

### 1.3 CNI Selection

| CNI | `networkType` | Status | Description |
|:----|:-------------|:-------|:------------|
| **OVN-Kubernetes** (Chosen) | `OVNKubernetes` | ✅ WORKING | Configured as default network provider. Deployed and managed natively by the HyperShift operator. Nodes register as `Ready` automatically. |

---

## 2. Deployment Steps

### Step 1: Create OIDC S3 Credentials Secret in HCP namespaces

The S3 OIDC bucket is created by Terraform and shared across clusters. The HCP operator OIDC credentials secret must exist in both operator namespaces (`local-cluster` and `hypershift`) for reconciliation to succeed.

```bash
mkdir -p /tmp/payu-hcp-setup
cat > /tmp/payu-hcp-setup/aws-credentials <<EOF
[default]
aws_access_key_id = $(aws configure get aws_access_key_id)
aws_secret_access_key = $(aws configure get aws_secret_access_key)
EOF

for NS in local-cluster hypershift; do
  oc create secret generic hypershift-operator-oidc-provider-s3-credentials \
    --from-literal=bucket=oidc-storage-payu-shared-955370087474 \
    --from-literal=region=ap-southeast-1 \
    --from-file=credentials=/tmp/payu-hcp-setup/aws-credentials \
    -n ${NS} --insecure-skip-tls-verify=true
done
```

### Step 2: Apply Terraform (creates VPCs, subnets, IAM roles, OIDC providers)

```bash
cd infrastructure/foundation/hostedcluster/terraform
terraform init
terraform apply -auto-approve
```

This provisions:
- VPC and Subnets for `payu-onprem` (`10.200.0.0/16`) and `payu-cloud` (`10.201.0.0/16`).
- Shared S3 OIDC bucket (`oidc-storage-payu-shared-955370087474`).
- Per-cluster IAM roles (CPO, registry, ingress, KCC, CNCC, EBS CSI, NodePool).
- OIDC providers pointing to regional endpoints.
  > [!NOTE]
  > The OIDC Provider `client_id_list` in Terraform must include both `sts.amazonaws.com` and `openshift`. This ensures tokens projected by the guest cluster `registry` pod (using the `openshift` audience) are accepted by AWS STS when accessing S3 bucket storage.

### Step 3: Deploy MutatingWebhook for Token Audience Fix

HyperShift Operator `35cddf08` has a bug where `token-minter` sidecars hardcode `--token-audience=openshift` instead of `sts.amazonaws.com` (needed for AWS IRSA role assumption). We deploy a mutating webhook that patches this argument to `sts.amazonaws.com`.

> [!NOTE]
> **OCP 4.15 template issue**: For OCP 4.15 guest clusters (e.g. `payu-onprem`), the default `ingress-operator` template doesn't include the `--token-audience` argument at all. The mutating webhook handles this by automatically **appending** `--token-audience=sts.amazonaws.com` if the argument is missing from the container spec.

1. **Deploy Webhook Pods and Service**:
   ```bash
   ./infrastructure/foundation/hostedcluster/scripts/deploy-webhook.sh
   ```
   *Note: This script automatically generates a TLS certificate, uploads it, base64-encodes the certificate in **PEM format** (fixing the binary DER parsing issue), and configures the `MutatingWebhookConfiguration`.*

2. **Pre-create and label the control plane namespaces**:
   ```bash
   oc create namespace clusters-payu-onprem --insecure-skip-tls-verify=true || true
   oc label ns clusters-payu-onprem purpose=hcp-control-plane --overwrite --insecure-skip-tls-verify=true
   oc create namespace clusters-payu-cloud --insecure-skip-tls-verify=true || true
   oc label ns clusters-payu-cloud purpose=hcp-control-plane --overwrite --insecure-skip-tls-verify=true
   ```

### Step 4: Create Per-Cluster Secrets

Extract the global pull secret and create pull-secrets and etcd encryption keys inside the `clusters` namespace:

```bash
oc get secret pull-secret -n openshift-config -o jsonpath='{.data.\.dockerconfigjson}' | base64 -d > pull-secret-temp.json

for KEY in payu-onprem payu-cloud; do
  oc create secret generic ${KEY}-pull-secret -n clusters \
    --from-file=.dockerconfigjson=pull-secret-temp.json \
    --type=kubernetes.io/dockerconfigjson \
    --insecure-skip-tls-verify=true
  openssl rand 32 | oc create secret generic ${KEY}-etcd-encryption-key -n clusters \
    --from-file=key=/dev/stdin \
    --insecure-skip-tls-verify=true
done
rm -f pull-secret-temp.json
```

### Step 5: Generate and Apply Manifests

1. **Generate cluster manifests**:
   ```bash
   ./infrastructure/foundation/hostedcluster/scripts/generate-manifests.sh
   ```
2. **Apply HostedClusters and NodePools**:
   ```bash
   oc apply -f infrastructure/foundation/hostedcluster/manifests/hostedcluster-payu-onprem.yaml
   oc apply -f infrastructure/foundation/hostedcluster/manifests/hostedcluster-payu-cloud.yaml
   oc apply -f infrastructure/foundation/hostedcluster/manifests/nodepools-payu-onprem.yaml
   oc apply -f infrastructure/foundation/hostedcluster/manifests/nodepools-payu-cloud.yaml
   ```

### Step 6: Fix NodePool `iam:PassRole` permissions

Add explicit inline `iam:PassRole` policies to the IAM roles to allow the Cluster API provider to pass roles to EC2 worker nodes:

```bash
for CLUSTER in payu-onprem payu-cloud; do
  cat > /tmp/passrole-$CLUSTER.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": "arn:aws:iam::559050246145:role/${CLUSTER}-node-pool",
      "Condition": {"StringEqualsIfExists": {"iam:PassedToService": "ec2.amazonaws.com"}}
    },
    {
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": "arn:aws:iam::559050246145:role/${CLUSTER}-cloud-controller",
      "Condition": {"StringEqualsIfExists": {"iam:PassedToService": "ec2.amazonaws.com"}}
    }
  ]
}
EOF
  aws iam put-role-policy --role-name ${CLUSTER}-node-pool \
    --policy-name ${CLUSTER}-pass-role-explicit \
    --policy-document file:///tmp/passrole-$CLUSTER.json
done
```

---

## 3. Post-Deployment Verification

### 3.1 Monitor Control Plane Availability

Monitor the progress of the clusters until they show `Available=True` and `Progressing=False`:

```bash
watch 'oc get hostedcluster -n clusters'
```

### 3.2 Access Guest Clusters

1. **Extract Kubeconfig files**:
   ```bash
   oc get secret payu-onprem-admin-kubeconfig -n clusters -o jsonpath='{.data.kubeconfig}' | base64 -d > payu-onprem.kubeconfig
   oc get secret payu-cloud-admin-kubeconfig -n clusters -o jsonpath='{.data.kubeconfig}' | base64 -d > payu-cloud.kubeconfig
   ```
2. **Retrieve cluster endpoint and check node status**:
   ```bash
   # payu-onprem Nodes
   oc get nodes --kubeconfig=payu-onprem.kubeconfig --insecure-skip-tls-verify=true
   
   # payu-cloud Nodes
   oc get nodes --kubeconfig=payu-cloud.kubeconfig --insecure-skip-tls-verify=true
   ```
   *Nodes will boot in AWS, join automatically via OVN-Kubernetes, and transition to `Ready` status.*

### 3.3 Diagnostic Health Checks (Automated)

We have provided a comprehensive diagnostic script to immediately identify setup errors, webhook certificate parsing issues, OIDC misconfigurations, and stuck NodePools:

```bash
./infrastructure/foundation/hostedcluster/scripts/verify-health.sh
```

This script automatically runs and prints a report on the following health checks:
1. **Operator OIDC Credentials**: Verifies `hypershift-operator-oidc-provider-s3-credentials` exists in both operator namespaces (`local-cluster` and `hypershift`) with correct bucket and region parameters.
2. **MutatingWebhook CA Bundle Format**: Verifies the webhook serving certificate CA bundle is in valid PEM Base64 format (not corrupted or binary DER).
3. **Webhook Pods & Endpoints**: Checks if the `hcp-audience-fixer` service is active and backed by running pods.
4. **CAPI Pod Mutation**: Validates if CAPI provider pods have been successfully mutated to use `--token-audience=sts.amazonaws.com`.
5. **HostedCluster & NodePool Status**: Pulls live status tables and flags common AWS permission/credential errors (e.g., `AWSMachineTemplateSpec` generation errors).

---

## 4. Teardown / Destruction

Always delete HostedClusters using the APIs so that HyperShift terminates AWS resources cleanly before destroying Terraform.

```bash
# 1. Delete NodePools and HostedClusters
oc delete -f infrastructure/foundation/hostedcluster/manifests/nodepools-payu-onprem.yaml --wait=false --insecure-skip-tls-verify=true
oc delete -f infrastructure/foundation/hostedcluster/manifests/hostedcluster-payu-onprem.yaml --wait=false --insecure-skip-tls-verify=true
oc delete -f infrastructure/foundation/hostedcluster/manifests/nodepools-payu-cloud.yaml --wait=false --insecure-skip-tls-verify=true
oc delete -f infrastructure/foundation/hostedcluster/manifests/hostedcluster-payu-cloud.yaml --wait=false --insecure-skip-tls-verify=true

# 2. Wait for namespaces to terminate
watch 'oc get ns | grep clusters-payu'

# 3. Clean up Terraform infrastructure
cd infrastructure/foundation/hostedcluster/terraform
terraform destroy -auto-approve
```

---

## 5. Troubleshooting Reference

> [!TIP]
> **First Line of Defense**: Before starting manual troubleshooting, run the automated health check script:
> ```bash
> ./infrastructure/foundation/hostedcluster/scripts/verify-health.sh
> ```
> This script instantly pinpoints issues with OIDC credentials, webhook TLS certificate formats, routing status, and cluster provisioning errors.

### 5.1 Common Webhook Issues

* **Symptom**: `Failed calling webhook, failing open audience-fixer.hcp.payu.io: ... unable to parse bytes as PEM block`.
  * **Root Cause**: The CA bundle applied in `MutatingWebhookConfiguration` was in binary DER format instead of PEM Base64.
  * **Fix**: Re-encode the certificate using `base64` directly on the PEM file without openssl DER flags, and rollout-restart `hcp-audience-fixer` deployment in `payu-system` namespace.

* **Symptom**: `capi-provider` pod is not mutated and retains `--token-audience=openshift`.
  * **Root Cause**: The pod was created before the namespace was labeled, or the webhook was not healthy yet.
  * **Fix**: Force the ReplicaSet to recreate the pod by deleting the current one: `oc delete pod -l control-plane=capi-provider-controller-manager -n <namespace>`.

* **Symptom**: Ingress operator or other pods fail with `WebIdentityErr: failed to retrieve credentials` due to `Incorrect token audience` on OCP 4.15.
  * **Root Cause**: On OCP 4.15 (e.g. `payu-onprem`), the default template for the `ingress-operator`'s `token-minter` container does not define the `--token-audience` argument at all, causing it to fall back to `openshift`.
  * **Fix**: Use the updated webhook logic (`hcp-audience-fixer-script` configmap updated via `deploy-webhook.sh`/`patch-cm-complete.py`) which automatically appends `--token-audience=sts.amazonaws.com` if it's missing, and restart the `ingress-operator` pod.

* **Symptom**: `image-registry` Cluster Operator stuck in `Available=False` on guest clusters (`payu-onprem` / `payu-cloud`).
  * **Root Cause**:
    1. The mutating webhook originally forced `--token-audience=sts.amazonaws.com` on all `token-minter` containers, including `client-token-minter` / `apiserver-token-minter`. These containers need to authenticate to the guest API server using the OIDC token audience, so forcing the AWS audience caused `Unauthorized` errors on the guest API server.
    2. The AWS IAM OIDC Provider client ID list in Terraform lacked the `openshift` audience, causing AWS STS to reject OIDC tokens projected by the `registry` pod inside the guest cluster when trying to write to the shared S3 storage bucket.
  * **Fix**:
    1. Update the webhook logic to only mutate AWS STS token-minters (e.g. `token-minter`, `cloud-token-minter`), leaving `client-token` or `apiserver` token-minters alone. This is handled dynamically by checking if the container name contains `client-token` or `apiserver`.
    2. Ensure the Terraform OIDC config includes the `openshift` client ID (audience) and re-apply Terraform.

### 5.2 AWS STS Authentication Issues

* **Symptom**: `ValidAWSIdentityProvider=False` stuck on HostedCluster status.
  * **Root Cause**: STS rejects IRSA tokens because the IAM OIDC provider thumbprint is wrong, or the audience does not include `sts.amazonaws.com`.
  * **Fix**: Ensure the Terraform `tls_certificate` data source points to the regional S3 endpoint (`https://s3.<region>.amazonaws.com`), and verify the client ID includes `sts.amazonaws.com` on the IAM OpenID Connect provider.

### 5.3 OIDC and Operator Reconciliation Issues

* **Symptom**: NodePool stuck with `failed to generate AWSMachineTemplateSpec: the default security group for the HostedCluster has not been created`. HostedCluster status reports `ValidOIDCConfiguration=False` and/or `ReconciliationSucceeded=False` with error `hypershift wasn't configured with a S3 bucket or credentials...`.
  * **Root Cause**: The secret `hypershift-operator-oidc-provider-s3-credentials` is missing from the operator namespaces (either `local-cluster` or `hypershift`).
  * **Fix**:
    1. Recreate the secret using AWS credentials pointing to the shared S3 OIDC bucket:
       ```bash
       mkdir -p /tmp/payu-hcp-setup && cat > /tmp/payu-hcp-setup/aws-credentials <<EOF
       [default]
       aws_access_key_id = \$(aws configure get aws_access_key_id)
       aws_secret_access_key = \$(aws configure get aws_secret_access_key)
       EOF
       oc create secret generic hypershift-operator-oidc-provider-s3-credentials --from-literal=bucket=oidc-storage-payu-shared-955370087474 --from-literal=region=ap-southeast-1 --from-file=credentials=/tmp/payu-hcp-setup/aws-credentials -n local-cluster --insecure-skip-tls-verify=true
       oc create secret generic hypershift-operator-oidc-provider-s3-credentials --from-literal=bucket=oidc-storage-payu-shared-955370087474 --from-literal=region=ap-southeast-1 --from-file=credentials=/tmp/payu-hcp-setup/aws-credentials -n hypershift --insecure-skip-tls-verify=true
       rm -rf /tmp/payu-hcp-setup
       ```
    2. Restart the HyperShift operator deployment to clear the cache and force re-reconciliation:
       ```bash
       oc rollout restart deployment/operator -n hypershift --insecure-skip-tls-verify=true
       ```

