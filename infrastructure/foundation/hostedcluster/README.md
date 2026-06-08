# PayU — Hosted Control Plane (HCP) Provisioning

> **Hosted Cluster**: PayU (`payu-dev`)
> **Management Cluster**: `local-cluster` (OCP 4.18.42, ap-southeast-1)
> **Provisioning Tool**: OpenShift Hypershift + AWS

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│  Management Cluster (local-cluster)                  │
│  ┌───────────────────────────────────────────────┐  │
│  │  hosted-control-planes namespace              │  │
│  │  ┌─────────────────┐  ┌──────────────────┐   │  │
│  │  │ Control Plane    │  │ Control Plane     │   │  │
│  │  │ (dev-rt7zf)      │  │ (payu-rt7zf)      │   │  │
│  │  │ API, etcd, OAuth │  │ API, etcd, OAuth  │   │  │
│  │  └─────────────────┘  └──────────────────┘   │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  ┌───────────────────────────────────────────────┐  │
│  │  Worker Nodes (AWS EC2)                       │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐    │  │
│  │  │ dev      │  │ payu     │  │ payu     │    │  │
│  │  │ m6a.2xl  │  │ m6a.2xl  │  │ m6a.2xl  │    │  │
│  │  └──────────┘  └──────────┘  └──────────┘    │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## 📋 Prerequisites

### 1. AWS IAM Roles (dibuat manual atau via Terraform)

Roles yang diperlukan untuk PayU HCP (prefix: `payu-rt7zf`):

| Role | ARN Pattern | Purpose |
|:-----|:------------|:--------|
| `payu-rt7zf-control-plane-operator` | `arn:aws:iam::579147609200:role/payu-rt7zf-control-plane-operator` | Control plane operator |
| `payu-rt7zf-openshift-image-registry` | `arn:aws:iam::579147609200:role/payu-rt7zf-openshift-image-registry` | Image registry S3 |
| `payu-rt7zf-openshift-ingress` | `arn:aws:iam::579147609200:role/payu-rt7zf-openshift-ingress` | Ingress (NLB/ALB) |
| `payu-rt7zf-cloud-controller` | `arn:aws:iam::579147609200:role/payu-rt7zf-cloud-controller` | Cloud controller |
| `payu-rt7zf-cloud-network-config-controller` | `arn:aws:iam::579147609200:role/payu-rt7zf-cloud-network-config-controller` | Network config |
| `payu-rt7zf-node-pool` | `arn:aws:iam::579147609200:role/payu-rt7zf-node-pool` | Node pool management |
| `payu-rt7zf-aws-ebs-csi-driver-controller` | `arn:aws:iam::579147609200:role/payu-rt7zf-aws-ebs-csi-driver-controller` | EBS CSI storage |

> **Buat dengan AWS CLI** (gunakan `create-iam-roles.sh`) atau lihat Terraform module di `infrastructure/foundation/terraform/aws/`.

### 2. Secrets pada Management Cluster

```bash
# Pull secret (dari cloud.openshift.com atau existing)
oc create secret generic payu-pull-secret \
  -n local-cluster \
  --from-file=.dockerconfigjson=<(oc get secret development-pull-secret -n local-cluster -o jsonpath='{.data.\.dockerconfigjson}' | base64 -d) \
  --type=kubernetes.io/dockerconfigjson

# Etcd encryption key
openssl rand -base64 32 | oc create secret generic payu-etcd-encryption-key \
  -n local-cluster \
  --from-file=key=/dev/stdin

# SSH key (opsional)
ssh-keygen -t ed25519 -f /tmp/payu-ssh -N "" -q
oc create secret generic payu-ssh-key \
  -n local-cluster \
  --from-file=id_ed25519.pub=/tmp/payu-ssh.pub
```

## 🚀 Provisioning

### Step 1: Apply HostedCluster

```bash
oc apply -f hostedcluster-payu.yaml
```

Verify:
```bash
oc get hostedcluster payu-dev -n local-cluster -w
# Tunggu sampai: PROGRESS=Completed, AVAILABLE=True
```

### Step 2: Apply NodePools (with autoscaling)

```bash
oc apply -f nodepools-payu.yaml
```

Verify:
```bash
oc get nodepool -n local-cluster -l hypershift.openshift.io/cluster=payu-dev -w
# Tunggu sampai semua node READY
```

### Step 3: Get kubeconfig

```bash
oc get secret -n local-cluster payu-dev-admin-kubeconfig \
  -o jsonpath='{.data.kubeconfig}' | base64 -d > /tmp/payu-kubeconfig

export KUBECONFIG=/tmp/payu-kubeconfig
oc get nodes
```

### Step 4: Get kubeadmin password

```bash
oc get secret -n local-cluster payu-dev-kubeadmin-password \
  -o jsonpath='{.data.password}' | base64 -d
echo
```

## ⚙️ Autoscaling Configuration

| Parameter | Value | Keterangan |
|:----------|:------|:-----------|
| **Min replicas** | 3 | Minimum untuk HA |
| **Max replicas** | 6 | Auto-scale berdasarkan load |
| **Instance type** | m6a.2xlarge | 8 vCPU, 32 GiB RAM |
| **Root volume** | 120 GiB gp3 | Encrypted by default |
| **Zones** | ap-southeast-1a, 1b, 1c | Multi-AZ untuk resilience |

**NodePool sizing rationale untuk 23 services + 5 simulators:**

| Komponen | vCPU | Memory |
|:---------|:-----|:-------|
| 23 backend services (JVM) | ~12 CPU | ~24 GB |
| 5 simulators (Quarkus) | ~2 CPU | ~4 GB |
| PostgreSQL (Crunchy PGO) | ~1 CPU | ~2 GB |
| Keycloak (RHBK) | ~1 CPU | ~2 GB |
| Kafka (AMQ Streams) | ~2 CPU | ~4 GB |
| DataGrid caching | ~1 CPU | ~2 GB |
| Monitoring (Prometheus/Grafana) | ~1 CPU | ~2 GB |
| Overhead + buffer | ~4 CPU | ~8 GB |
| **Total minimum** | **~24 CPU** | **~48 GB** |

Dengan autoscaling 3-6 node × m6a.2xlarge (8vCPU/32GB):
- 3 node = 24vCPU / 96GB (cukup untuk baseline)
- 6 node = 48vCPU / 192GB (cukup untuk peak load)

## 🔄 Reference: Cluster `development` (Existing)

| Parameter | Value |
|:----------|:------|
| InfraID | `dev-rt7zf` |
| OCP Version | 4.18.43 |
| Region | ap-southeast-1 |
| Base domain | sandbox2356.opentlc.com |
| Subnet | subnet-0067a49544aa0bb1b |
| Worker | 1× m6a.2xlarge, 120Gi gp3 |
| OIDC | S3 (`oidc-storage-rt7zf`) |

## 📁 Related Files

- `hostedcluster-payu.yaml` — HostedCluster CR
- `nodepools-payu.yaml` — NodePool CRs dengan autoscaling
- `create-iam-roles.sh` — Script untuk membuat AWS IAM roles
- `infrastructure/foundation/terraform/aws/` — Terraform infrastructure modules
