# PayU HCP — `payu-onprem` (4.18) + `payu-cloud` (4.20) Deployment Guide

> **Hosted Clusters**: `payu-onprem` (v4.18.43, 1 node) + `payu-cloud` (v4.20.24, 1 node)
> **Management Cluster**: `payu-8tmf2` (OCP 4.20.24, MCE 2.11.2)
> **Platform**: AWS ap-southeast-1 (dedicated VPC per cluster, shared OIDC bucket)
> **CNI**: Cilium 1.16.5 (via `networkType: Other` — OVN-K had `to-br-int` patch port bug on single-node, fixed in iter 32)
> **Last Updated**: 2026-06-16 (iter 32)
> **References**: [OCP 4.20 HCP Docs](https://docs.redhat.com/en/documentation/openshift_container_platform/4.20/html/hosted_control_planes/) | [ROSA Best Practices](https://cloud.redhat.com/experts/rosa/best-practices-recommendations/) | [Cilium on OpenShift](https://docs.cilium.io/en/stable/installation/k8s-install-openshift-okd/)

---

## 0. Executive Summary (iter 32)

| Item | Value |
|:-----|:------|
| AWS Account | `559050246145` |
| Region | `ap-southeast-1` |
| HCP operator version | `35cddf08d3e492ec2b328a832a60a463407dd556` (MCE 2.11.2) |
| **Workaround for HCP 35cddf08** | `payu-system/hcp-audience-fixer` MutatingWebhook (patches `cloud-token-minter` sidecar to use `audience=sts.amazonaws.com`) |
| Terraform | `infrastructure/foundation/hostedcluster/terraform/` — `for_each` multi-cluster with shared OIDC bucket, dedicated VPCs |
| OIDC bucket | `oidc-storage-payu-shared-559050246145` (shared, per-cluster sub-paths) |
| `payu-onprem` | 4.18.43, 1× m6a.2xlarge, ap-southeast-1a, v1.31.14 |
| `payu-cloud` | 4.20.24, 1× m6a.2xlarge, ap-southeast-1a, v1.33.12 |
| Final state | **Both HCPs AVAILABLE, NodePool 1/1 Ready, node Ready** |

---

## 1. Pre-requisites

### 1.1 Management Cluster (ap-southeast-1)

| Account | Region | Activity | Validation | Status |
|:--------|:-------|:---------|:-----------|:-------|
| 559050246145 | ap-southeast-1 | Check MCE Operator | `multicluster-engine.v2.11.2` Succeeded | PASSED |
| 559050246145 | ap-southeast-1 | Check `local-cluster` managedcluster | `AVAILABLE=True` | PASSED |
| 559050246145 | ap-southeast-1 | Check HyperShift Operator | 2 pods `Running` in `hypershift` ns | PASSED |
| 559050246145 | ap-southeast-1 | Check HCP CLI | `hcp version` → `35cddf08...` | PASSED |
| 559050246145 | ap-southeast-1 | Check pull-secret | `oc get secret pull-secret -n openshift-config` exists | PASSED |

### 1.2 Networking & CIDR Allocation

| Network | `payu-onprem` | `payu-cloud` | Overlap |
|:--------|:--------------|:-------------|:--------|
| VPC CIDR | `10.200.0.0/16` (dedicated) | `10.201.0.0/16` (dedicated) | NO |
| clusterNetwork | `10.132.0.0/14` | `10.136.0.0/14` | NO |
| serviceNetwork | `172.31.0.0/16` | `172.32.0.0/16` | NO |
| Public subnet (1a) | `10.200.0.0/20` | `10.201.0.0/20` | NO |
| Worker node private IP | `10.200.2.71` (v1) / `10.200.15.203` (v2) | `10.201.2.83` (v1) / `10.201.5.138` (v2) | NO |
| Base domain | `payu.ocp.fajjjar.my.id` | `payu.ocp.fajjjar.my.id` | shared |
| Private Route53 zone | `Z0688851VIBKG68U8DFU` | `Z0688851VIBKG68U8DFU` | shared |
| Public Route53 zone | `Z0716734HV77ZJQGV03V` | `Z0716734HV77ZJQGV03V` | shared |

> [!IMPORTANT]
> **Dedicated VPC per cluster (not shared)** — Per iter 32 decision, each HCP gets its own VPC to avoid:
> * AWS API throttling from multiple cluster controllers
> * Subnet tag collisions (`kubernetes.io/cluster/<id> = shared`)
> * DNS cache contention on VPC resolver

### 1.3 CNI Selection

| CNI | `networkType` | Status | Why |
|:----|:-------------|:-------|:----|
| **OVN-Kubernetes** | `OVNKubernetes` | ❌ FAILED | `ovnkube-controller` waits for OVS port `*to-br-int` on `br-ex` — port not created by HCP bootstrap on single-node. Known HCP bug. |
| **Cilium** (chosen) | `Other` | ✅ WORKING | Manual install via Helm. Required for HCP single-node in this env. |

> **Why we chose Cilium over OVN-K**:
> 1. HCP 35cddf08's RHCOS bootstrap does NOT create the `br-ex` patch port to `br-int` on single-node workers
> 2. The `ovnkube-controller` init script loops forever waiting for this port (90 retries × 2s = 3 min)
> 3. Reference `DEPLOYMENT.md` §5.2 also hit this and switched to Cilium
> 4. The fix: `networkType: Other` + install Cilium 1.16.5 via Helm with `cni.confPath=/etc/kubernetes/cni/net.d`
> 5. Added `cni-fixer` DaemonSet that copies CNI config to all paths kubelet/multus check

---

## 2. Deployment

### Step 1: Create S3 OIDC Bucket (SHARED across clusters)

> Per HCP docs, the OIDC bucket is shared. We use one bucket with per-cluster sub-paths.

```bash
# Single shared bucket (Terraform creates this)
aws s3api create-bucket --bucket oidc-storage-payu-shared-559050246145 \
  --create-bucket-configuration LocationConstraint=ap-southeast-1

# Public read policy (required for OIDC discovery)
cat > /tmp/oidc-bucket-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "PublicRead",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::oidc-storage-payu-shared-559050246145/*"
  }]
}
EOF
aws s3api put-bucket-policy --bucket oidc-storage-payu-shared-559050246145 \
  --policy file:///tmp/oidc-bucket-policy.json
```

| Status | Activity |
|:-------|:---------|
| PASSED | S3 bucket created in ap-southeast-1 |
| PASSED | Public read policy applied |

### Step 2: Create OIDC S3 Credentials Secret in HCP namespace

```bash
mkdir -p /tmp/payu-hcp-setup
cat > /tmp/payu-hcp-setup/aws-credentials <<EOF
[default]
aws_access_key_id = $(aws configure get aws_access_key_id)
aws_secret_access_key = $(aws configure get aws_secret_access_key)
EOF

oc create secret generic hypershift-operator-oidc-provider-s3-credentials \
  --from-literal=bucket=oidc-storage-payu-shared-559050246145 \
  --from-literal=region=ap-southeast-1 \
  --from-file=credentials=/tmp/payu-hcp-setup/aws-credentials \
  -n local-cluster
```

| Status | Activity |
|:-------|:---------|
| PASSED | Secret `hypershift-operator-oidc-provider-s3-credentials` in `local-cluster` ns |

### Step 3: Apply Terraform (creates VPCs, subnets, IAM roles, OIDC providers)

```bash
cd /home/ubuntu/payu/infrastructure/foundation/hostedcluster/terraform
terraform init -upgrade
terraform apply -auto-approve
```

**Resources created (64 total) per cluster**:
- 1× VPC (10.200.0.0/16 or 10.201.0.0/16)
- 1× Public subnet (ap-southeast-1a)
- 1× Internet Gateway
- 1× Route table + association
- 1× Worker security group (`<cluster>-vpc-worker-sg`, ingress 0.0.0.0/0)
- 7× IAM roles per cluster: `control-plane-operator`, `openshift-image-registry`, `openshift-ingress`, `cloud-controller`, `cloud-network-config-controller`, `aws-ebs-csi-driver-controller`, `node-pool`
- 1× HCP CLI role per cluster
- 1× IAM instance profile per cluster
- 1× IAM OIDC provider per cluster (shared bucket, per-cluster sub-path)
- 1× Shared OIDC S3 bucket (single bucket, both clusters)
- 8× IAM role policy attachments (AmazonEC2FullAccess, etc.)

**Critical Terraform fix**: The `tls_certificate` data source must use `https://s3.<region>.amazonaws.com` (regional endpoint), NOT the bucket-specific hostname. Otherwise the OIDC thumbprint is wrong and STS rejects IRSA tokens.

| Status | Activity |
|:-------|:---------|
| PASSED | 64 resources applied |
| PASSED | OIDC provider thumbprints validated against actual S3 regional cert (`00ed4cfa17a3ffd7165f54d5ff28cf82e49caf45`) |

### Step 4: Apply HCP Workaround — MutatingWebhook for `cloud-token-minter` audience

> **HCP 35cddf08 bug**: The `cloud-token-minter` sidecar in HCP-managed pods hardcodes `--token-audience=openshift`. STS rejects this — needs `sts.amazonaws.com`. Affects CPO, KCC, CNCC, EBS CSI pods → role assumption fails → `WebIdentityErr` → NodePool stuck on "default security group not created".

**Fix**: Python MutatingWebhook that patches the `cloud-token-minter` args via JSONPatch admission review.

```bash
# Create namespace + ConfigMap with webhook Python script
oc create namespace payu-system
oc create configmap hcp-audience-fixer-script \
  --from-file=webhook.py=<(cat <<'PYEOF'
#!/usr/bin/env python3
import json, base64, os
from http.server import BaseHTTPRequestHandler, HTTPServer

# Map service-account-name to the right audience (sts.amazonaws.com for IRSA)
SA_TO_AUDIENCE = {
    "control-plane-operator": "sts.amazonaws.com",
    "cloud-network-config-controller": "sts.amazonaws.com",
    "kube-controller-manager": "sts.amazonaws.com",
    "aws-cloud-controller-manager": "sts.amazonaws.com",
    "csi-driver": "sts.amazonaws.com",
    "node-pool": "sts.amazonaws.com",
    "cluster-csi-drivers": "sts.amazonaws.com",
    "aws-ebs-csi-driver-controller": "sts.amazonaws.com",
    "openshift-ingress": "sts.amazonaws.com",
    "router": "sts.amazonaws.com",
}

def patch_containers(containers):
    patches = []
    for i, c in enumerate(containers or []):
        name = c.get("name", "")
        if not any(s in name for s in ("token-minter", "cloud-token")):
            continue
        args = c.get("args") or []
        new_args = []
        sa_name = ""
        for x in args:
            if isinstance(x, str) and x.startswith("--service-account-name="):
                sa_name = x.split("=", 1)[1]
        changed = False
        for a in args:
            if isinstance(a, str) and a.startswith("--token-audience="):
                target_aud = SA_TO_AUDIENCE.get(sa_name, "sts.amazonaws.com")
                new_args.append(f"--token-audience={target_aud}")
                changed = True
            else:
                new_args.append(a)
        if changed:
            patches.append({"op": "replace", "path": f"/spec/containers/{i}/args", "value": new_args})
    return patches

class H(BaseHTTPRequestHandler):
    def do_POST(self):
        n = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(n)
        adm = json.loads(body)
        req = adm.get("request", {})
        pod = req.get("object", {})
        spec = pod.get("spec", {})
        all_containers = spec.get("containers", []) + spec.get("initContainers", [])
        patches = patch_containers(all_containers)
        resp = {
            "apiVersion": "admission.k8s.io/v1",
            "kind": "AdmissionReview",
            "response": {
                "uid": req.get("uid"),
                "allowed": True,
                "patchType": "JSONPatch",
                "patch": base64.b64encode(json.dumps(patches).encode()).decode() if patches else None,
            },
        }
        out = json.dumps(resp).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(out)))
        self.end_headers()
        self.wfile.write(out)
    def log_message(self, *a, **k): pass

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8443))
    srv = HTTPServer(("0.0.0.0", port), H)
    cert = os.environ.get("TLS_CERT", "/tls/tls.crt")
    key = os.environ.get("TLS_KEY", "/tls/tls.key")
    if os.path.exists(cert) and os.path.exists(key):
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        ctx.load_cert_chain(cert, key)
        srv.socket = ctx.wrap_socket(srv.socket, server_side=True)
    srv.serve_forever()
PYEOF
)
# Generate TLS cert (self-signed for payu-system)
mkdir -p /tmp/payu-hcp-setup/webhook-tls
openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
  -keyout /tmp/payu-hcp-setup/webhook-tls/tls.key -out /tmp/payu-hcp-setup/webhook-tls/tls.crt \
  -subj "/CN=hcp-audience-fixer.payu-system.svc" \
  -addext "subjectAltName=DNS:hcp-audience-fixer.payu-system.svc,DNS:hcp-audience-fixer.payu-system.svc.cluster.local"

oc create configmap hcp-audience-fixer-tls \
  --from-file=tls.crt=/tmp/payu-hcp-setup/webhook-tls/tls.crt \
  --from-file=tls.key=/tmp/payu-hcp-setup/webhook-tls/tls.key \
  -n payu-system

# Deploy webhook (registry.access.redhat.com/ubi9/python-311)
oc apply -f - <<'EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hcp-audience-fixer
  namespace: payu-system
spec:
  replicas: 2
  selector:
    matchLabels:
      app: hcp-audience-fixer
  template:
    metadata:
      labels:
        app: hcp-audience-fixer
    spec:
      containers:
      - name: webhook
        image: registry.access.redhat.com/ubi9/python-311:latest
        command: ["python3", "/scripts/webhook.py"]
        ports:
        - containerPort: 8443
        env:
        - name: PORT
          value: "8443"
        volumeMounts:
        - name: scripts
          mountPath: /scripts
        - name: tls
          mountPath: /tls
        resources:
          requests: {cpu: 50m, memory: 64Mi}
          limits:   {cpu: 200m, memory: 256Mi}
      volumes:
      - name: scripts
        configMap: {name: hcp-audience-fixer-script}
      - name: tls
        configMap: {name: hcp-audience-fixer-tls}
---
apiVersion: v1
kind: Service
metadata:
  name: hcp-audience-fixer
  namespace: payu-system
spec:
  selector: {app: hcp-audience-fixer}
  ports: [{port: 443, targetPort: 8443}]
  type: ClusterIP
EOF

# Apply MutatingWebhookConfiguration (caBundle = base64 of PEM cert)
CA_BUNDLE=$(openssl x509 -in /tmp/payu-hcp-setup/webhook-tls/tls.crt -outform DER | base64 -w0)
python3 -c "
import json, os
config = {
    'apiVersion': 'admissionregistration.k8s.io/v1',
    'kind': 'MutatingWebhookConfiguration',
    'metadata': {'name': 'hcp-audience-fixer'},
    'webhooks': [{
        'name': 'audience-fixer.hcp.payu.io',
        'sideEffects': 'None',
        'admissionReviewVersions': ['v1'],
        'namespaceSelector': {'matchLabels': {'purpose': 'hcp-control-plane'}},
        'rules': [{'operations': ['CREATE', 'UPDATE'], 'apiGroups': [''], 'apiVersions': ['v1'], 'resources': ['pods']}],
        'clientConfig': {
            'service': {'name': 'hcp-audience-fixer', 'namespace': 'payu-system', 'path': '/', 'port': 443},
            'caBundle': '$CA_BUNDLE'
        },
        'failurePolicy': 'Ignore',
        'reinvocationPolicy': 'Never'
    }]
}
print(json.dumps(config))
" | oc apply -f -

# Label HCP namespaces (auto-created when HCs are applied)
for NS in clusters-payu-onprem clusters-payu-cloud; do
  oc label ns $NS purpose=hcp-control-plane --overwrite
done
```

| Status | Activity |
|:-------|:---------|
| PASSED | Webhook Python deployment in `payu-system` (2 replicas) |
| PASSED | Self-signed TLS cert generated |
| PASSED | MutatingWebhookConfiguration created with `namespaceSelector` for `purpose=hcp-control-plane` |
| PASSED | HCP namespaces labeled with `purpose=hcp-control-plane` |

### Step 5: Create per-cluster secrets

```bash
# Pull secret from management cluster
oc get secret pull-secret -n openshift-config -o jsonpath='{.data.\.dockerconfigjson}' | base64 -d > /tmp/payu-hcp-setup/pull-secret.json

# Per-cluster secrets
for KEY in payu-onprem payu-cloud; do
  oc create secret generic ${KEY}-pull-secret -n clusters \
    --from-file=.dockerconfigjson=/tmp/payu-hcp-setup/pull-secret.json \
    --type=kubernetes.io/dockerconfigjson
  openssl rand 32 | oc create secret generic ${KEY}-etcd-encryption-key -n clusters \
    --from-file=key=/dev/stdin
done
```

| Status | Activity |
|:-------|:---------|
| PASSED | 4 secrets in `clusters` ns (2× pull-secret, 2× etcd-encryption-key) |

### Step 6: Apply HostedCluster + NodePool manifests

The manifests are generated from Terraform outputs:

```bash
# Generate manifests (uses shared_oidc_bucket + per-cluster IDs from Terraform output)
bash /home/ubuntu/payu/infrastructure/foundation/hostedcluster/scripts/generate-manifests.sh

# Apply HCs
oc apply -f infrastructure/foundation/hostedcluster/manifests/hostedcluster-payu-onprem.yaml
oc apply -f infrastructure/foundation/hostedcluster/manifests/hostedcluster-payu-cloud.yaml

# Apply NodePools (after HCs are created)
oc apply -f infrastructure/foundation/hostedcluster/manifests/nodepools-payu-onprem.yaml
oc apply -f infrastructure/foundation/hostedcluster/manifests/nodepools-payu-cloud.yaml

# Label HCP namespaces for webhook (if not auto-applied)
for NS in clusters-payu-onprem clusters-payu-cloud; do
  oc label ns $NS purpose=hcp-control-plane --overwrite
done
```

**Key HC manifest settings** (see `manifests/hostedcluster-payu-{onprem,cloud}.yaml`):

```yaml
spec:
  networkType: Other              # Cilium (NOT OVNKubernetes)
  controllerAvailabilityPolicy: SingleReplica
  infrastructureAvailabilityPolicy: SingleReplica
  release:
    image: quay.io/openshift-release-dev/ocp-release:4.18.43-multi  # or 4.20.24-multi
  platform:
    aws:
      region: ap-southeast-1
      endpointAccess: Public
  networking:
    clusterNetwork: [{cidr: 10.132.0.0/14}]  # or 10.136.0.0/14
    serviceNetwork: [{cidr: 172.31.0.0/16}]  # or 172.32.0.0/16
    machineNetwork: [{cidr: 10.0.0.0/16}]
  configuration:
    ingress:
      loadBalancer:
        platform:
          aws:
            type: NLB
```

| Status | Activity |
|:-------|:---------|
| PASSED | Both HCs created |
| PASSED | Both NodePools created |
| PASSED | HCP control planes AVAILABLE after ~2 min |

### Step 7: Fix CPO audience on first deployment

The webhook patches NEW pods. Existing pods (created before webhook) need manual restart:

```bash
# Wait for webhook to patch any new CPO pods
sleep 30
# Force-restart any existing CPO pod with wrong audience
for NS in clusters-payu-onprem clusters-payu-cloud; do
  for POD in $(oc get pod -n $NS -l app=control-plane-operator -o name 2>&1 | cut -d'/' -f2); do
    AUD=$(oc exec -n $NS $POD -c control-plane-operator -- \
      cat /var/run/secrets/openshift/serviceaccount/token 2>&1 | head -1 | cut -d'.' -f2 | base64 -d 2>/dev/null | \
      python3 -c "import json,sys; print(json.load(sys.stdin).get('aud',['?'])[0])" 2>/dev/null)
    if [ "$AUD" != "sts.amazonaws.com" ]; then
      echo "  ✗ $NS/$POD audience=$AUD — deleting to force re-create (webhook will fix)"
      oc delete pod -n $NS $POD --wait=false
    fi
  done
done
sleep 60
```

| Status | Activity |
|:-------|:---------|
| PASSED | CPO pods restarted with correct audience |
| PASSED | `ValidAWSIdentityProvider=True` confirmed |

### Step 8: Fix `iam:PassRole` on node-pool roles

> **Issue**: CAPI controller can't `iam:PassRole` on the node-pool role. `AmazonEC2FullAccess` has `iam:PassRole` with condition `iam:PassedToService: ec2.amazonaws.com` but for some reason it's not being satisfied. Result: `UnauthorizedOperation: ... not authorized to perform: iam:PassRole`.

**Fix**: Add explicit inline `iam:PassRole` policy to each `<cluster>-node-pool` role.

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

| Status | Activity |
|:-------|:---------|
| PASSED | Inline `iam:PassRole` policy added to both `payu-onprem-node-pool` and `payu-cloud-node-pool` |
| PASSED | EC2 instances launched successfully after fix |

### Step 9: Install Cilium CNI in both guest clusters

> **Why Cilium, not OVN-K**: See §1.3. The HCP with `networkType: Other` does NOT deploy any CNI. We install Cilium manually.

```bash
export PATH=/home/ubuntu/bin:$PATH  # helm binary location

# Get kubeconfig with --server override (bypass management cluster DNS cache)
KAS_ON=$(oc get hostedcontrolplane payu-onprem -n clusters-payu-onprem -o jsonpath='{.status.controlPlaneEndpoint.host}')
KAS_ON_IP=$(getent hosts $KAS_ON 2>/dev/null | awk '{print $1}' | head -1)
cat > /tmp/payu-onprem-cilium.kubeconfig <<EOF
apiVersion: v1
kind: Config
clusters:
- cluster: {insecure-skip-tls-verify: true, server: https://${KAS_ON_IP}:6443}
  name: payu-onprem
contexts:
- context: {cluster: payu-onprem, user: admin}
  name: admin@payu-onprem
current-context: admin@payu-onprem
users:
- name: admin
  user:
    client-certificate-data: $(oc get secret payu-onprem-admin-kubeconfig -n clusters -o jsonpath='{.data.kubeconfig}' | base64 -d | python3 -c "import yaml,sys; print(yaml.safe_load(sys.stdin)['users'][0]['user']['client-certificate-data'])")
    client-key-data: $(oc get secret payu-onprem-admin-kubeconfig -n clusters -o jsonpath='{.data.kubeconfig}' | base64 -d | python3 -c "import yaml,sys; print(yaml.safe_load(sys.stdin)['users'][0]['user']['client-key-data'])")
EOF
chmod 600 /tmp/payu-onprem-cilium.kubeconfig

# Install Cilium (critical params for RHCOS)
helm repo add cilium https://helm.cilium.io/
helm repo update
helm install cilium cilium/cilium --version 1.16.5 \
  --namespace kube-system \
  --kubeconfig /tmp/payu-onprem-cilium.kubeconfig \
  --set securityContext.privileged=true \
  --set kubeProxyReplacement=true \
  --set ipam.mode=cluster-pool \
  --set ipam.operator.clusterPoolIPv4PodCIDRList=10.132.0.0/14 \
  --set ipam.operator.clusterPoolIPv4MaskSize=24 \
  --set cni.confPath=/etc/kubernetes/cni/net.d \
  --set cni.binPath=/opt/cni/bin \
  --set hubble.enabled=false
# Repeat for payu-cloud with 10.136.0.0/14
```

| Status | Activity |
|:-------|:---------|
| PASSED | Cilium installed on payu-onprem |
| PASSED | Cilium installed on payu-cloud |

### Step 10: Apply cni-fixer DaemonSet (handles kubelet --cni-conf-dir mismatch)

> **Issue**: The kubelet looks at `/etc/kubernetes/cni/net.d/` (HCP custom path) but Cilium pod's init containers may write to different paths, AND the kubelet caches "no CNI file" state for a long time.

**Fix**: A DaemonSet that polls for CNI config and copies it to all expected paths. Also sends SIGKILL to kubelet to force re-evaluation.

```bash
oc apply -f /home/ubuntu/payu/infrastructure/foundation/hostedcluster/manifests/cni-fixer-daemonset.yaml
# Apply to BOTH clusters (replace kubeconfig)
```

The DaemonSet:
1. Watches for CNI config at `/host/etc/cni/net.d/`
2. Copies to `/host/etc/kubernetes/cni/net.d/` (kubelet) and `/host/run/multus/cni/net.d/` (multus)
3. Sends SIGKILL to kubelet (`/usr/bin/kubelet`) — systemd restarts it, clearing cache
4. Keeps running to fix future Cilium pod restarts

| Status | Activity |
|:-------|:---------|
| PASSED | cni-fixer DaemonSet deployed to both guest clusters |

### Step 11: Fix RHCOS `/opt/cni` symlink (one-time per worker)

> **Issue**: On RHCOS, `/opt/cni` is a **symlink** to `/usr/lib/opt/cni`, NOT a directory. Cilium's `mount-cgroup` init container tries `mkdir /opt/cni/bin` which fails with `mkdir /opt/cni: file exists` because the symlink already exists.

**Fix**: `rm /opt/cni` (removes the symlink), then Cilium init can `mkdir` it fresh.

```bash
# Create a debug pod on each worker
cat > /tmp/check-cni.yaml <<EOF
apiVersion: v1
kind: Pod
metadata: {name: check-cni, namespace: default}
spec:
  restartPolicy: Never
  nodeName: <worker-node-name>
  hostNetwork: true
  containers:
  - name: check
    image: registry.access.redhat.com/ubi9/ubi-minimal:latest
    command: ["sleep", "120"]
    volumeMounts: [{name: host, mountPath: /host}]
  volumes: [{name: host, hostPath: {path: /}}]
EOF
oc apply -f /tmp/check-cni.yaml
sleep 15

# Remove /opt/cni (the symlink)
oc exec -n default check-cni -- rm -rfv /host/opt/cni

# Force-restart the failed cilium pod
oc -n kube-system delete pod <failed-cilium-pod>
```

| Status | Activity |
|:-------|:---------|
| PASSED | `/opt/cni` symlink removed on payu-cloud worker |
| PASSED | Cilium pod `cilium-cdkvh` 1/1 Running |

---

## 3. Post-Deployment

### 3.1 Verify Cluster

```bash
# Wait for AVAILABLE (typical 5-10 min after Cilium install)
watch 'oc get hostedcluster -n clusters; oc get nodepool -n clusters'

# Get guest cluster kubeconfig
oc get secret payu-onprem-admin-kubeconfig -n clusters -o jsonpath='{.data.kubeconfig}' | base64 -d > /tmp/payu-onprem.kubeconfig
KAS=$(oc get hostedcontrolplane payu-onprem -n clusters-payu-onprem -o jsonpath='{.status.controlPlaneEndpoint.host}')
KAS_IP=$(getent hosts $KAS 2>/dev/null | awk '{print $1}' | head -1)

# Check nodes (use --server override to bypass mgmt cluster DNS cache)
kubectl --kubeconfig=/tmp/payu-onprem.kubeconfig --server="https://${KAS_IP}:6443" --insecure-skip-tls-verify get nodes

# Check cluster operators
kubectl --kubeconfig=/tmp/payu-onprem.kubeconfig --server="https://${KAS_IP}:6443" --insecure-skip-tls-verify get co
```

**Final verified state (2026-06-16T22:00Z)**:

| Cluster | Version | Node | Status | COs True | Console URL |
|:--------|:--------|:-----|:-------|:---------|:-------------|
| payu-onprem | 4.18.43 | ip-10-200-15-203 Ready (v1.31.14) | 1/1 | 18/22 | `https://console-openshift-console.apps.payu-onprem.payu.ocp.fajjjar.my.id` |
| payu-cloud | 4.20.24 | ip-10-201-5-138 Ready (v1.33.12) | 1/1 | 14/22 | `https://console-openshift-console.apps.payu-cloud.payu.ocp.fajjjar.my.id` |

**Kubeadmin passwords**:
- payu-onprem: `VhTof-QMkdY-Q5ti9-xFnXa`
- payu-cloud: `KBqnK-44doy-jgt38-nsGcZ`

### 3.2 AWS Infrastructure Created

```
VPCs:
  payu-onprem: vpc-0990e89bb39594fda (10.200.0.0/16)
  payu-cloud:  vpc-01f49ec611641c9c5 (10.201.0.0/16)
Subnets:
  payu-onprem: subnet-0eaeeed588ae98882 (10.200.0.0/20, ap-southeast-1a, public)
  payu-cloud:  subnet-0b51076311ac7c47d (10.201.0.0/20, ap-southeast-1a, public)
Worker SGs:
  payu-onprem: sg-086d3a7cf216e08d0
  payu-cloud:  sg-0db5e13e27baaa944
S3 OIDC:
  oidc-storage-payu-shared-559050246145 (shared, per-cluster sub-paths)
  Issuer URLs:
    payu-onprem: https://oidc-storage-payu-shared-559050246145.s3.ap-southeast-1.amazonaws.com/payu-onprem
    payu-cloud:  https://oidc-storage-payu-shared-559050246145.s3.ap-southeast-1.amazonaws.com/payu-cloud
IAM Roles (per cluster × 7 + 1 HCP CLI + 1 OIDC provider):
  - <cluster>-control-plane-operator
  - <cluster>-openshift-image-registry
  - <cluster>-openshift-ingress
  - <cluster>-cloud-controller
  - <cluster>-cloud-network-config-controller
  - <cluster>-aws-ebs-csi-driver-controller
  - <cluster>-node-pool
  - <cluster>-hcp-cli-role
  - OIDC provider per cluster
EC2 Workers:
  payu-onprem: i-<id> m6a.2xlarge (10.200.x.x) — terminated and recreated during debugging
  payu-cloud:  i-<id> m6a.2xlarge (10.201.x.x) — terminated and recreated during debugging
```

### 3.3 Workarounds Deployed

| Workaround | Where | Why |
|:----------|:------|:----|
| `hcp-audience-fixer` MutatingWebhook | `payu-system` ns | Fix HCP 35cddf08 hardcoded `--token-audience=openshift` |
| `cni-fixer` DaemonSet | Both guest `kube-system` ns | Copy CNI config to kubelet's `--cni-conf-dir`, restart kubelet to clear cache |
| Inline `iam:PassRole` policy | `<cluster>-node-pool` IAM roles | Fix CAPI controller `iam:PassRole` UnauthorizedOperation |

---

## 4. Destroy

```bash
# 1. Delete HCP (this terminates EC2 cleanly via HCP API — NEVER use aws ec2 terminate-instances directly!)
oc delete hostedcluster payu-onprem -n clusters
oc delete hostedcluster payu-cloud -n clusters

# 2. Wait for HCP namespaces to terminate
watch 'oc get ns | grep clusters-payu'

# 3. Remove webhook + cni-fixer
oc delete mutatingwebhookconfiguration hcp-audience-fixer
oc delete -n payu-system deployment hcp-audience-fixer
oc delete -n payu-system configmap hcp-audience-fixer-script hcp-audience-fixer-tls
oc delete ns payu-system

# 4. Destroy Terraform
cd /home/ubuntu/payu/infrastructure/foundation/hostedcluster/terraform
terraform destroy -auto-approve
```

> [!CAUTION]
> **NEVER terminate EC2 via `aws ec2 terminate-instances` directly**. This causes `InstanceUnexpectedTermination` warning on the AWSMachine, HCP marks the Machine as `Failed`, and HCP does NOT create a new Machine. You must delete the Failed Machine manually to recover. Always terminate via HCP/Machine API (`oc delete hostedcluster` or `oc delete machine`).

---

## 5. Troubleshooting

### 5.1 Bugs Hit in iter 32 (with fixes)

| # | Symptom | Root Cause | Fix | Time Lost |
|:-:|:--------|:-----------|:----|:----------|
| 1 | HCP CPO `WebIdentityErr failed to retrieve credentials` | HCP 35cddf08 `cloud-token-minter` hardcodes `--token-audience=openshift` (should be `sts.amazonaws.com` for IRSA) | Python MutatingWebhook in `payu-system` patches the sidecar args | ~3h |
| 2 | OIDC provider thumbprint mismatch (STS `InvalidIdentityToken`) | Terraform `tls_certificate` data source used wrong URL; resulting thumbprint `06b25927c...` didn't match `s3.ap-southeast-1.amazonaws.com` cert (`00ed4cfa17a3ffd7165f54d5ff28cf82e49caf45`) | Fix Terraform to use `https://s3.<region>.amazonaws.com`; manual `aws iam update-open-id-connect-provider-thumbprint` | ~1h |
| 3 | EC2 `UnauthorizedOperation: ... iam:PassRole ... payu-onprem-node-pool` | CAPI controller couldn't `iam:PassRole` the node-pool role; `AmazonEC2FullAccess` v5 `iam:PassRole` condition `iam:PassedToService: ec2.amazonaws.com` wasn't being met | Add explicit inline `iam:PassRole` policy to both `<cluster>-node-pool` roles | ~1h |
| 4a | OVN-K `ovnkube-controller` loops forever waiting for OVS port `*to-br-int` on `br-ex` | HCP 35cddf08 RHCOS bootstrap does NOT create the `br-ex` patch port on single-node workers | Switch to Cilium (`networkType: Other`) | ~2h |
| 4b | Cilium `mount-cgroup` init fails: `failed to mkdir /opt/cni/bin: mkdir /opt/cni: file exists` | On RHCOS, `/opt/cni` is a symlink to `/usr/lib/opt/cni`, not a directory; Cilium's `mkdir` fails on the existing symlink | `rm -rfv /opt/cni` to remove symlink, then Cilium init can `mkdir` it fresh | ~30 min |
| 4c | Node stuck `NotReady: no CNI configuration file in /etc/kubernetes/cni/net.d/` | Kubelet's `--cni-conf-dir` is `/etc/kubernetes/cni/net.d/` (HCP custom path); Cilium pod's CNI config is at `/etc/cni/net.d/` (default); ALSO kubelet caches "no file" for a long time | `cni-fixer` DaemonSet: copies CNI config to all expected paths + sends SIGKILL to kubelet (systemd restarts it) | ~3h |
| 5 | `InstanceUnexpectedTermination` warning → Machine `Failed` → HCP won't create new Machine | I called `aws ec2 terminate-instances` directly instead of `oc delete machine` | **NEVER** terminate EC2 directly; always use HCP/Machine API | ~4h |

### 5.2 CNI Issues (Cilium)

| Issue | Root Cause | Fix |
|:------|:-----------|:----|
| Cilium pod `Init:CreateContainerError: failed to mkdir /opt/cni/bin: mkdir /opt/cni: file exists` | `/opt/cni` is a symlink on RHCOS, not a directory | Use a privileged debug pod to `rm -rfv /opt/cni` on the worker, then delete the failed Cilium pod to force re-create |
| Node stuck `NotReady: no CNI configuration file in /etc/kubernetes/cni/net.d/` | Kubelet `--cni-conf-dir` is HCP custom path; Cilium pod's CNI config written to `/etc/cni/net.d/` (default) | Apply `cni-fixer` DaemonSet (included in repo) that copies config to all paths + restarts kubelet |
| Multus `CrashLoopBackOff: failed to find the cluster master CNI plugin: could not find a plugin configuration in /host/run/multus/cni/net.d` | Multus looks for CNI config at `/run/multus/cni/net.d/` (default) | `cni-fixer` DaemonSet also copies to this path |
| `networkType: Cilium` not recognized | HyperShift does NOT support `Cilium` as networkType | Use `networkType: Other` (tells CPO to skip deploying any CNI) and install manually via Helm |

### 5.3 OVN-K Issues (don't use — left for reference)

| Issue | Root Cause | Fix |
|:------|:-----------|:----|
| `ovnkube-controller` loops forever waiting for OVS port `*to-br-int` on `br-ex` | HCP 35cddf08 RHCOS bootstrap does NOT create the `br-ex` patch port on single-node workers | Switch to Cilium (recommended) OR manually create the OVS port via a first-boot MachineConfig: `ovs-vsctl --may-exist add-port br-int patch-br-int -- set Interface patch-br-int type=patch options:peer=patch-to-br-int && ovs-vsctl --may-exist add-port br-ex patch-to-br-int -- set Interface patch-to-br-int type=patch options:peer=patch-br-int` |
| `ValidAWSIdentityProvider=False` stuck | CPO pod's `cloud-token-minter` minting token with wrong audience | Fix via `hcp-audience-fixer` MutatingWebhook (see Step 4) |

### 5.4 General Issues

| Issue | Root Cause | Fix |
|:------|:-----------|:----|
| `VpcLimitExceeded` | Too many VPCs | Delete unused or request increase |
| `AddressLimitExceeded` | Too many EIPs | `aws ec2 release-address --allocation-id <ID>` |
| `RouteAlreadyExists` | Leftover VPC | Delete old VPC first |
| `Secret not found` | Wrong namespace | HCP CLI uses `clusters` ns |
| `WebIdentityErr` | OIDC docs missing in S3 OR wrong audience | Re-upload JWKS; check `cloud-token-minter` audience = `sts.amazonaws.com` |
| Node not joining | SG missing inbound rules | Add `IpProtocol=-1, CidrIp=<VPC_CIDR>` to worker SG |
| `InvalidIdentityToken` (assumed-role not authorized) | Missing `sts.amazonaws.com` audience in IAM OIDC provider | `aws iam add-client-id-to-open-id-connect-provider --open-id-connect-provider-arn <ARN> --client-id sts.amazonaws.com` |
| EC2 instance profile / role cannot be assumed | IAM NodePool management role trust relationship missing `ec2.amazonaws.com` | Add `ec2.amazonaws.com` service principal to role trust policy |
| `SyncLoadBalancerFailed` (could not find suitable subnets) | Shared VPC subnets lack cluster discovery tag | Add `Key=kubernetes.io/cluster/<infra-id>,Value=shared` to all subnets |
| Console operator degraded with `no such host` | AWS VPC resolver (`10.0.0.2`) cached negative lookup (NXDOMAIN) | Patch guest DNS operator: `upstreamResolvers.upstreams[0] = {type: Network, address: 8.8.8.8}` |
| Machine `Failed: InstanceUnexpectedTermination` | EC2 terminated outside of HCP API (e.g. `aws ec2 terminate-instances`) | Delete the Failed Machine: `oc delete machine <name> -n <hcp-ns>`. HCP will create a new one. |

---

## 6. Key Files & References

| File | Purpose |
|:-----|:--------|
| `terraform/main.tf` | Multi-cluster `for_each` over `var.clusters` map |
| `terraform/terraform.tfvars` | Cluster definitions: name, version, CIDRs, AZ, instance type |
| `terraform/variables.tf` | Variable schema (clusters map, region, common tags) |
| `terraform/outputs.tf` | Per-cluster outputs: VPC IDs, subnet IDs, role ARNs, OIDC issuer URLs |
| `terraform/modules/vpc/` | Dedicated VPC + public subnet + IGW + worker SG per cluster |
| `terraform/modules/iam/` | 7 IRSA roles + HCP CLI role + OIDC provider per cluster (shared OIDC bucket) |
| `manifests/hostedcluster-payu-onprem.yaml` | HC manifest for payu-onprem (4.18.43) |
| `manifests/hostedcluster-payu-cloud.yaml` | HC manifest for payu-cloud (4.20.24) |
| `manifests/nodepools-payu-onprem.yaml` | NodePool for payu-onprem (1× m6a.2xlarge, ap-southeast-1a) |
| `manifests/nodepools-payu-cloud.yaml` | NodePool for payu-cloud (1× m6a.2xlarge, ap-southeast-1a) |
| `manifests/cni-fixer-daemonset.yaml` | DaemonSet that copies CNI config + restarts kubelet |
| `scripts/generate-manifests.sh` | Generate HC + NodePool YAMLs from `terraform output -json` |
| `../payu-system/hcp-audience-fixer` | MutatingWebhook (Python) for `cloud-token-minter` audience fix |

| Document | URL |
|:---------|:----|
| OCP 4.20 HCP Docs | https://docs.redhat.com/en/documentation/openshift_container_platform/4.20/html/hosted_control_planes/ |
| ROSA Best Practices | https://cloud.redhat.com/experts/rosa/best-practices-recommendations/ |
| HyperShift GitHub | https://github.com/openshift/hypershift |
| Cilium on OpenShift | https://docs.cilium.io/en/stable/installation/k8s-install-openshift-okd/ |
| RFE-3137 (HCP 35cddf08 IRSA bug) | https://issues.redhat.com/browse/OCPBUGS-31370 |
