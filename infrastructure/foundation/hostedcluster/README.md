# PayU — Hosted Control Plane (HCP) Provisioning

> **Hosted Cluster**: PayU (`payu-dev`)
> **Management Cluster**: `local-cluster` (OCP 4.18.42, MCE 2.8.7)
> **Namespace**: `clusters` (HCP CLI default)
> **Control Plane Namespace**: `clusters-payu-dev`
> **Provisioning Tool**: HCP CLI + OpenShift Hypershift

---

## Quick Start

```bash
# 1. Create S3 bucket for OIDC
aws s3api create-bucket --bucket oidc-storage-payu-dev \
  --create-bucket-configuration LocationConstraint=ap-southeast-1

# 2. Create OIDC S3 credentials secret
oc create secret generic hypershift-operator-oidc-provider-s3-credentials \
  --from-literal=bucket=oidc-storage-payu-dev \
  --from-literal=region=ap-southeast-1 \
  --from-file=credentials=${HOME}/.aws/credentials \
  -n local-cluster

# 3. Create IAM role for HCP CLI
aws iam create-role --role-name payu-dev-hcp-cli-role \
  --assume-role-policy-document file://trust.json
aws iam put-role-policy --role-name payu-dev-hcp-cli-role \
  --policy-name payu-dev-policy \
  --policy-document file://policy.json

# 4. Get STS credentials
aws sts get-session-token --duration-seconds 86400 --output json > /tmp/sts-creds.json

# 5. Generate YAML
hcp create cluster aws \
  --name payu-dev --infra-id payu-dev \
  --base-domain sandbox2356.opentlc.com \
  --sts-creds /tmp/sts-creds.json \
  --pull-secret /tmp/pull-secret.json \
  --region ap-southeast-1 \
  --role-arn arn:aws:iam::579147609200:role/payu-dev-hcp-cli-role \
  --node-pool-replicas 1 \
  --render > /tmp/payu-dev.yaml

# 6. Fix CIDR, HA, version, and NLB
sed -i 's/controllerAvailabilityPolicy: HighlyAvailable/controllerAvailabilityPolicy: SingleReplica/' /tmp/payu-dev.yaml
sed -i 's/cidr: 10.132.0.0\/14/cidr: 10.136.0.0\/14/' /tmp/payu-dev.yaml
sed -i 's/cidr: 172.31.0.0\/16/cidr: 172.32.0.0\/16/' /tmp/payu-dev.yaml
sed -i 's/4.22.0-multi/4.18.43-multi/' /tmp/payu-dev.yaml
# Add NLB ingress config (like install-config.yaml lbType: NLB)
# Already in manifests/hostedcluster-payu.yaml: configuration.ingress.loadBalancer.platform.aws.type: NLB
sed -i 's/4.22.0-multi/4.18.43-multi/' /tmp/payu-dev.yaml

# 7. Create secrets and apply
oc create secret generic payu-dev-pull-secret -n clusters \
  --from-file=.dockerconfigjson=/tmp/pull-secret.json --type=kubernetes.io/dockerconfigjson
openssl rand 32 | oc create secret generic payu-dev-etcd-encryption-key -n clusters \
  --from-file=key=/dev/stdin
oc apply -f /tmp/payu-dev.yaml

# 8. Verify
oc get hostedcluster payu-dev -n clusters -w
```

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  Management Cluster (local-cluster)                  │
│  ┌───────────────────────────────────────────────┐  │
│  │  clusters namespace                           │  │
│  │  ┌─────────────────┐                          │  │
│  │  │ HostedCluster    │  payu-dev               │  │
│  │  │ NodePool         │  payu-dev-ap-southeast-1a│  │
│  │  └─────────────────┘                          │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  ┌───────────────────────────────────────────────┐  │
│  │  clusters-payu-dev namespace                  │  │
│  │  ┌─────────────────┐  ┌──────────────────┐   │  │
│  │  │ Control Plane    │  │ kube-apiserver   │   │  │
│  │  │ (CPO, etcd, CAPI)│  │ etcd, ingress    │   │  │
│  │  └─────────────────┘  └──────────────────┘   │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  ┌───────────────────────────────────────────────┐  │
│  │  Worker Node (AWS EC2)                        │  │
│  │  ┌──────────┐                                │  │
│  │  │ payu-dev │  m5.large, 120Gi gp3           │  │
│  │  └──────────┘                                │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## Configuration Summary

| Parameter | Value |
|:----------|:------|
| Cluster Name | `payu-dev` |
| Namespace | `clusters` |
| InfraID | `payu-dev` |
| OCP Version | `4.18.43-multi` |
| Region | `ap-southeast-1` |
| Base Domain | `sandbox2356.opentlc.com` |
| VPC | `vpc-0a17e396dc91f3a02` (auto-created by HCP CLI) |
| Subnet | `subnet-02398ff0ca4a471ef` (auto-created) |
| Network Type | `OVNKubernetes` |
| Cluster Network | `10.136.0.0/14` |
| Service Network | `172.32.0.0/16` |
| Endpoint Access | `Public` |
| Controller Availability | `SingleReplica` |
| Etcd Storage | `8Gi gp3-csi` |
| Node Instance Type | `m5.large` |
| Node Root Volume | `120 GiB gp3` |
| Node Replicas | `1` |

---

## CIDR Allocation

| Network | `development` Cluster | `payu-dev` Cluster | Overlap |
|:--------|:----------------------|:-------------------|:--------|
| clusterNetwork | `10.132.0.0/14` | `10.136.0.0/14` | NO |
| serviceNetwork | `172.31.0.0/16` | `172.32.0.0/16` | NO |
| machineNetwork | `10.0.0.0/16` | `10.0.0.0/16` | Shared VPC |

---

## Access Commands

```bash
# Get kubeconfig
oc get secret payu-dev-admin-kubeconfig -n clusters \
  -o jsonpath='{.data.kubeconfig}' | base64 -d > /tmp/payu-dev-kubeconfig

# Access hosted cluster
export KUBECONFIG=/tmp/payu-dev-kubeconfig
oc get nodes
oc get co

# Get kubeadmin password
oc get secret payu-dev-kubeadmin-password -n clusters \
  -o jsonpath='{.data.password}' | base64 -d && echo
```

**Console**: https://console-openshift-console.apps.payu-dev.sandbox2356.opentlc.com
**kubeadmin**: `vzcte-nX5SW-42HqT-8zaM7`

---

## Files

```
hostedcluster/
├── README.md                          # Quick start & overview
├── DEPLOYMENT.md                      # Full deployment log + best practices
├── manifests/
│   ├── hostedcluster-payu.yaml        # HostedCluster CR
│   └── nodepools-payu.yaml            # NodePool CR
├── iam/
│   ├── policy.json                    # IAM permissions for HCP CLI
│   └── trust.json                     # IAM trust policy for HCP CLI role
└── scripts/
    ├── create-iam-roles.sh            # Create IAM roles
    ├── create-oidc-bucket.sh          # Create S3 OIDC bucket
    └── setup-prerequisites.sh         # One-shot prerequisites setup
```

---

## References

| Document | URL |
|:---------|:----|
| OCP 4.18 HCP Docs | https://docs.redhat.com/en/documentation/openshift_container_platform/4.18/html/hosted_control_planes/ |
| HCP CLI Download | https://hcp-cli-download-multicluster-engine.apps.cluster-rt7zf.rt7zf.sandbox2356.opentlc.com/linux/amd64/hcp.tar.gz |
