# PayU HCP — `payu-dev` Deployment Guide

> **Hosted Cluster**: `payu-dev`
> **Management Cluster**: `local-cluster` (OCP 4.22, MCE 2.8.7+)
> **Platform**: AWS us-east-1 (shared VPC)
> **CNI**: Cilium 1.19+ (via `networkType: Other`) or OVN-Kubernetes (default)
> **Last Updated**: 2026-06-10
> **References**: [OCP 4.21 HCP Docs](https://docs.redhat.com/en/documentation/openshift_container_platform/4.21/html/hosted_control_planes/deploying-hosted-control-planes) | [ROSA Best Practices](https://cloud.redhat.com/experts/rosa/best-practices-recommendations/) | [Cilium on OpenShift](https://docs.cilium.io/en/stable/installation/k8s-install-openshift-okd/)

---

## 1. Pre-requisites

### 1.1 Management Cluster

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Check MCE Operator | `oc get csv -n multicluster-engine` | `multicluster-engine.v2.8.x` | 1 | PASSED | Required >= 2.5 |
| 579147609200 | us-east-1 | Check local-cluster | `oc get managedclusters local-cluster` | `AVAILABLE=True` | 1 | PASSED | Hub cluster must be managed |
| 579147609200 | us-east-1 | Check HyperShift Operator | `oc get pods -n hypershift` | 2 pods `Running` | 1 | PASSED | — |
| 579147609200 | us-east-1 | Check AWS CLI | `aws --version` | `aws-cli/2.x` | 1 | PASSED | — |
| 579147609200 | us-east-1 | Check aws-creds | `oc get secret aws-creds -n kube-system` | Secret exists | 1 | PASSED | — |
| 579147609200 | us-east-1 | Install HCP CLI | `curl -fsSL "<hcp-url>" -o /tmp/hcp.tar.gz && tar xzf /tmp/hcp.tar.gz -C /tmp && sudo mv /tmp/hcp /usr/local/bin/` | `hcp version` | 2 | PASSED | — |
| 579147609200 | us-east-1 | Validate Route53 zone | `aws route53 get-hosted-zone --id Z02597472Z6C8KKS12MZJ` | Zone exists | 1 | PASSED | `sandbox2356.opentlc.com` |

### 1.2 Networking & CIDR (Best Practice)

> Per [ROSA Best Practices](https://cloud.redhat.com/experts/rosa/best-practices-recommendations/): use non-overlapping CIDRs, separate public/private subnets, FQDN-only (no hardcoded IPs).

| Network | `development` Cluster | `payu-dev` Cluster | Overlap |
|:--------|:----------------------|:-------------------|:--------|
| VPC CIDR | `10.0.0.0/16` | `10.0.0.0/16` (shared) | OK |
| clusterNetwork | `10.132.0.0/14` | `10.136.0.0/14` | **NO** |
| serviceNetwork | `172.31.0.0/16` | `172.32.0.0/16` | **NO** |
| machineNetwork | `10.0.0.0/16` | `10.0.0.0/16` | Shared VPC (NOT Recommended) |

> [!WARNING]
> **Shared VPC Co-existence for Multiple Hosted Clusters is NOT Recommended**:
> Running multiple hosted clusters (e.g. `payu-onprem`, `payu-prod`, `payu-dev`) inside the exact same shared VPC subnets is strongly discouraged for production.
> * **AWS API Throttling**: Constant status checks by multiple cluster controllers (AWS Cloud Provider, VPC CNI, EBS CSI) trigger AWS API rate limits, causing administrative command hangs and reconciliation timeouts.
> * **Subnet Tagging Collisions**: Cloud providers auto-discover and override tags (`kubernetes.io/cluster/<cluster-id> = shared`) on the shared subnets, causing route and load balancer conflicts.
> * **DNS Cache Contention**: DNS query overload on the VPC resolver (`10.0.0.2`) leads to negative-cache lookup failures.
> 
> *Best Practice*: Deploy each hosted cluster in its own dedicated VPC to isolate API calls, subnet tags, and security boundaries.

> **Critical**: `10.133.0.0/14` == `10.132.0.0/14` (same CIDR). Always verify with bitwise AND.

### 1.3 CNI Selection

| CNI | `networkType` | Pros | Cons |
|:----|:-------------|:-----|:-----|
| **OVN-Kubernetes** (default) | `OVNKubernetes` | Zero config, fully supported by Red Hat | Limited advanced network policies |
| **Cilium** (recommended) | `Other` | eBPF-based, advanced L7 policies, kube-proxy replacement, Hubble observability | Manual install via Helm, RHCOS path override required |

> **Important**: HyperShift does NOT support `networkType: Cilium`. You must use `networkType: Other` which tells the control plane to skip deploying any CNI, then install Cilium manually via Helm post-deployment.

| Best Practice | Status | Remarks |
|:--------------|:-------|:--------|
| CNI selected | DONE | Cilium 1.19+ (`networkType: Other`) |
| Non-overlapping CIDRs | DONE | Verified (Shared VPC is NOT recommended for multi-cluster) |
| Min `/25` for single-AZ machine CIDR | DONE | `10.0.0.0/16` |
| DNS hostnames + support enabled | DONE | HCP CLI auto-enables |
| Separate public/private subnets | DONE | HCP CLI creates both |
| Use FQDNs, never hardcode IPs | TODO | Document all endpoints |

---

## 2. Deployment

Per [Red Hat Docs §4.1.1](https://docs.redhat.com/en/documentation/openshift_container_platform/4.18/html/hosted_control_planes/deploying-hosted-control-planes#hcp-aws-prepare_hcp-deploy-aws), HCP CLI handles ALL infrastructure (VPC, subnets, IAM roles, OIDC provider, Route53 zones).

### Step 1: Create S3 OIDC Bucket

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Create S3 Bucket | `aws s3api create-bucket --bucket oidc-storage-payu-dev-579147609200` | `aws s3api head-bucket --bucket oidc-storage-payu-dev-579147609200` | 1 | PASSED | — |
| 579147609200 | us-east-1 | Remove Public Access Block | `aws s3api delete-public-access-block --bucket oidc-storage-payu-dev-579147609200` | — | 1 | PASSED | Required for OIDC |
| 579147609200 | us-east-1 | Set Bucket Policy | `aws s3api put-bucket-policy --bucket oidc-storage-payu-dev-579147609200 --policy file://s3-policy.json` | `aws s3api get-bucket-policy --bucket oidc-storage-payu-dev-579147609200` | 1 | PASSED | `s3:GetObject` for `Principal: *` |

### Step 2: Create OIDC S3 Credentials Secret

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Create Secret | `oc create secret generic hypershift-operator-oidc-provider-s3-credentials --from-literal=bucket=oidc-storage-payu-dev-579147609200 --from-literal=region=us-east-1 --from-file=credentials=${HOME}/.aws/credentials -n local-cluster` | `oc get secret hypershift-operator-oidc-provider-s3-credentials -n local-cluster` | 1 | PASSED | Fields: `bucket`, `region`, `credentials` |

### Step 3: Create IAM Role for HCP CLI

> Per [Red Hat Docs §4.1.1.4](https://docs.redhat.com/en/documentation/openshift_container_platform/4.18/html/hosted_control_planes/deploying-hosted-control-planes#hcp-aws-create-role-sts-creds_hcp-deploy-aws): create IAM role with `trust.json` + `policy.json`.

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Create IAM Role | `aws iam create-role --role-name payu-dev-hcp-cli-role --assume-role-policy-document file://iam/trust.json --query "Role.Arn" --output text` | `aws iam get-role --role-name payu-dev-hcp-cli-role` | 1 | PASSED | `trust.json`: user can `sts:AssumeRole` |
| 579147609200 | us-east-1 | Attach Policy | `aws iam put-role-policy --role-name payu-dev-hcp-cli-role --policy-name payu-dev-policy --policy-document file://iam/policy.json` | `aws iam get-role-policy --role-name payu-dev-hcp-cli-role --policy-name payu-dev-policy` | 1 | PASSED | `policy.json`: EC2, ELB, IAM, R53, S3 |

### Step 4: Get STS Credentials

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Get STS Token | `aws sts get-session-token --duration-seconds 86400 --output json > /tmp/sts-creds.json` | `jq .Credentials.AccessKeyId /tmp/sts-creds.json` | 1 | PASSED | JSON format |
| 579147609200 | us-east-1 | Get Pull Secret | `oc get secret pull-secret -n openshift-config -o jsonpath='{.data.\.dockerconfigjson}' \| base64 -d > /tmp/pull-secret.json` | `wc -c /tmp/pull-secret.json` | 1 | PASSED | — |

### Step 5: Run HCP CLI

> Per [Red Hat Docs §4.1.3](https://docs.redhat.com/en/documentation/openshift_container_platform/4.18/html/hosted_control_planes/deploying-hosted-control-planes#hcp-aws-deploy-hc_hcp-deploy-aws)

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Render YAML | `hcp create cluster aws --name payu-dev --infra-id payu-dev --base-domain sandbox2356.opentlc.com --sts-creds /tmp/sts-creds.json --pull-secret /tmp/pull-secret.json --region us-east-1 --role-arn arn:aws:iam::579147609200:role/payu-dev-hcp-cli-role --node-pool-replicas 2 --render /tmp/payu-dev.yaml` | `/tmp/payu-dev.yaml` generated | 2 | PASSED | HCP CLI creates VPC, IAM, OIDC, R53 |
| 579147609200 | us-east-1 | Fix CIDR overlap | `sed -i 's/cidr: 10.132.0.0\/14/cidr: 10.136.0.0\/14/' /tmp/payu-dev.yaml && sed -i 's/cidr: 172.31.0.0\/16/cidr: 172.32.0.0\/16/' /tmp/payu-dev.yaml` | `grep cidr /tmp/payu-dev.yaml` | 1 | PASSED | Non-overlapping with `development` |
| 579147609200 | us-east-1 | Fix HA → SingleReplica | `sed -i 's/controllerAvailabilityPolicy: HighlyAvailable/controllerAvailabilityPolicy: SingleReplica/' /tmp/payu-dev.yaml` | `grep AvailabilityPolicy /tmp/payu-dev.yaml` | 1 | PASSED | Dev: no HA needed |
| 579147609200 | us-east-1 | Fix OCP version | `sed -i 's/4.22.0-multi/4.21.0-multi/' /tmp/payu-dev.yaml` | `grep release /tmp/payu-dev.yaml` | 1 | PASSED | MCE 2.8 max = 4.22 |
| 579147609200 | us-east-1 | Fix Ingress to NLB | Add `configuration.ingress.loadBalancer.platform.aws.type: NLB` to HostedCluster spec | `grep -A5 "ingress:" /tmp/payu-dev.yaml` | 1 | PASSED | Like `install-config.yaml` `lbType: NLB`. Avoids post-deploy CLB→NLB migration |
| 579147609200 | us-east-1 | Add Resource Tags | Add `resourceTags` to `spec.platform.aws` with `kubernetes.io/cluster`, `app.kubernetes.io/part-of`, `environment`, `cost-center`, `owner` | `grep -A10 "resourceTags:" /tmp/payu-dev.yaml` | 1 | PASSED | Max 25 user tags (AWS limit 50 - OCP reserved 25). Applied to EC2, ELB, EBS, VPC, etc. |
| 579147609200 | us-east-1 | Create Secrets | `oc create secret generic payu-dev-pull-secret -n clusters --from-file=.dockerconfigjson=/tmp/pull-secret.json --type=kubernetes.io/dockerconfigjson && openssl rand 32 \| oc create secret generic payu-dev-etcd-encryption-key -n clusters --from-file=key=/dev/stdin` | `oc get secrets -n clusters \| grep payu-dev` | 1 | PASSED | Etcd key: 32 raw bytes (AES-CBC) |
| 579147609200 | us-east-1 | Apply YAML | `oc apply -f /tmp/payu-dev.yaml` | `oc get hostedcluster payu-dev -n clusters` | 1 | PASSED | Creates NS, HostedCluster, NodePool |

### Step 5b: Install Cilium CNI (if `networkType: Other`)

> **Skip this step** if using `networkType: OVNKubernetes`. Only required when using Cilium CNI.
>
> When `networkType: Other` is set, the control plane operator does NOT deploy any CNI. Worker nodes will remain `NotReady` until Cilium is installed.
>
> ⚠️ **Critical Helm Parameters for RHCOS**:
> | Parameter | Value | Why |
> |:----------|:------|:----|
> | `cni.confPath` | `/etc/kubernetes/cni/net.d` | **RHCOS uses this path**, NOT the default `/etc/cni/net.d`. Nodes will stay `NotReady` if wrong. |
> | `cni.binPath` | `/opt/cni/bin` | Standard CNI binary path on RHCOS |
> | `ipam.operator.clusterPoolIPv4PodCIDRList` | `10.136.0.0/14` | Must match `spec.networking.clusterNetwork[0].cidr` in HostedCluster |
> | `kubeProxyReplacement` | `true` | Cilium replaces kube-proxy entirely (eBPF dataplane) |
> | `securityContext.privileged` | `true` | Required for eBPF programs on RHCOS |

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Get guest kubeconfig | `oc get secret payu-dev-admin-kubeconfig -n clusters -o jsonpath='{.data.kubeconfig}' \| base64 -d > /tmp/payu-dev.kubeconfig` | File exists | 1 | PASSED | — |
| 579147609200 | us-east-1 | Confirm nodes NotReady | `oc --kubeconfig /tmp/payu-dev.kubeconfig get nodes` | All nodes `NotReady` | 1 | PASSED | Expected — no CNI deployed yet |
| 579147609200 | us-east-1 | Add Cilium Helm repo | `helm repo add cilium https://helm.cilium.io/ && helm repo update` | Repo added | 1 | PASSED | — |
| 579147609200 | us-east-1 | Install Cilium 1.19 | `helm install cilium cilium/cilium --namespace kube-system --kubeconfig /tmp/payu-dev.kubeconfig --set securityContext.privileged=true --set kubeProxyReplacement=true --set ipam.mode=cluster-pool --set ipam.operator.clusterPoolIPv4PodCIDRList=10.136.0.0/14 --set ipam.operator.clusterPoolIPv4MaskSize=24 --set cni.confPath=/etc/kubernetes/cni/net.d --set cni.binPath=/opt/cni/bin` | `helm ls -n kube-system --kubeconfig /tmp/payu-dev.kubeconfig` shows `cilium` deployed | 2 | PASSED | See critical parameters table above |
| 579147609200 | us-east-1 | Verify Cilium agents | `oc --kubeconfig /tmp/payu-dev.kubeconfig get pods -n kube-system -l app.kubernetes.io/name=cilium-agent` | 1 pod per node, all `Running` | 2 | PASSED | DaemonSet: 1 agent per worker |
| 579147609200 | us-east-1 | Verify Cilium operator | `oc --kubeconfig /tmp/payu-dev.kubeconfig get pods -n kube-system -l app.kubernetes.io/name=cilium-operator` | `Running` | 1 | PASSED | — |
| 579147609200 | us-east-1 | Verify nodes Ready | `oc --kubeconfig /tmp/payu-dev.kubeconfig get nodes` | All nodes `Ready` | 2 | PASSED | Takes ~60s after Cilium starts |
| 579147609200 | us-east-1 | Cilium connectivity check | `oc --kubeconfig /tmp/payu-dev.kubeconfig exec -n kube-system ds/cilium -- cilium status --brief` | `OK` | 1 | PASSED | — |
| 579147609200 | us-east-1 | Fix CoreDNS upstream | `oc --kubeconfig /tmp/payu-dev.kubeconfig patch dns.operator.openshift.io default --type merge -p '{"spec":{"upstreamResolvers":{"upstreams":[{"type":"Network","address":"8.8.8.8","port":53}]}}}'` | `oc --kubeconfig /tmp/payu-dev.kubeconfig get dns.operator default -o jsonpath='{.spec.upstreamResolvers}'` | 1 | PASSED | AWS VPC resolver (`10.0.0.2`) caches NXDOMAIN up to 24h. Override to `8.8.8.8` to bypass stale cache |
| 579147609200 | us-east-1 | Find worker SG | `aws ec2 describe-instances --filters "Name=tag:kubernetes.io/cluster/payu-dev,Values=owned" --query "Reservations[].Instances[].SecurityGroups[0].GroupId" --output text --region us-east-1 \| head -1` | SG ID returned (e.g., `sg-0ede58a3da36ae517`) | 1 | PASSED | — |
| 579147609200 | us-east-1 | Add SG rule for NLB | `aws ec2 authorize-security-group-ingress --group-id $WORKER_SG --protocol -1 --cidr 10.0.0.0/16 --region us-east-1` | `aws ec2 describe-security-group-rules --filters "Name=group-id,Values=$WORKER_SG" --region us-east-1` shows `10.0.0.0/16` rule | 1 | PASSED | NLB preserves client source IP → SG must allow VPC CIDR, not just NLB SG |
| 579147609200 | us-east-1 | Get ingress NLB hostname | `oc --kubeconfig /tmp/payu-dev.kubeconfig get svc router-default -n openshift-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'` | NLB hostname returned | 1 | PASSED | — |
| 579147609200 | us-east-1 | Get NLB Hosted Zone ID | `aws elbv2 describe-load-balancers --query "LoadBalancers[?DNSName=='$NLB_HOST'].CanonicalHostedZoneId" --output text --region us-east-1` | Zone ID returned (e.g., `Z26RNL4JYFTOTI`) | 1 | PASSED | Needed for Route 53 Alias records |
| 579147609200 | us-east-1 | Create Route 53 change batch | `cat > /tmp/route53-apps.json` with `*.apps.payu-dev.sandbox2356.opentlc.com` A-Alias → NLB | File created | 1 | PASSED | — |
| 579147609200 | us-east-1 | Update PUBLIC zone DNS | `aws route53 change-resource-record-sets --hosted-zone-id <PUBLIC_ZONE_ID> --change-batch file:///tmp/route53-apps.json --region us-east-1` | `PENDING` → `INSYNC` | 1 | PASSED | Public zone for external access |
| 579147609200 | us-east-1 | Update PRIVATE zone DNS | `aws route53 change-resource-record-sets --hosted-zone-id <PRIVATE_ZONE_ID> --change-batch file:///tmp/route53-apps.json --region us-east-1` | `PENDING` → `INSYNC` | 1 | PASSED | **Critical**: Private zone for VPC-internal DNS. Without this, worker nodes get NXDOMAIN |
| 579147609200 | us-east-1 | Verify DNS resolve | `nslookup console-openshift-console.apps.payu-dev.sandbox2356.opentlc.com 8.8.8.8` | Resolves to NLB public IP | 1 | PASSED | — |
| 579147609200 | us-east-1 | Verify console accessible | `curl -skI https://console-openshift-console.apps.payu-dev.sandbox2356.opentlc.com` | `HTTP/1.1 200 OK` | 2 | PASSED | May need `--resolve` flag if local DNS cache is stale |

**Optional: KMS for etcd encryption** (production-grade, like ROSA):

```bash
# List available KMS keys
aws kms list-keys --query 'Keys[].KeyId' --output text

# Get KMS key ARN
KMS_KEY_ARN=$(aws kms describe-key --key-id <KEY_ID> --query 'KeyMetadata.Arn' --output text)

# Add to hcp create cluster aws command:
hcp create cluster aws \
  ... \
  --kms-key-arn ${KMS_KEY_ARN} \
  --root-volume-kms-key ${KMS_KEY_ARN} \
  --render /tmp/payu-dev.yaml
```

| Flag | Purpose | Default |
|:-----|:--------|:--------|
| `--kms-key-arn` | KMS key for etcd encryption | AES-CBC (generated key) |
| `--root-volume-kms-key` | KMS key for node root volume encryption | AWS managed key |

> **When to use KMS**: Production environments, compliance requirements (PCI-DSS, HIPAA), multi-tenant clusters. KMS provides centralized key management, automatic rotation, and audit trail via CloudTrail.

### Step 6: Verify Deployment

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Wait for Available | `oc get hostedcluster payu-dev -n clusters -w` | `AVAILABLE=True`, `PROGRESS=Completed` | 15 | PASSED | — |
| 579147609200 | us-east-1 | Check Control Plane | `oc get pods -n clusters-payu-dev` | 43 pods running | 5 | PASSED | kube-apiserver, etcd, CPO, CAPI |
| 579147609200 | us-east-1 | Check NodePool | `oc get nodepool -n clusters` | `CURRENT NODES = 2` | 10 | PASSED | — |
| 579147609200 | us-east-1 | Get kubeconfig | `oc get secret payu-dev-admin-kubeconfig -n clusters -o jsonpath='{.data.kubeconfig}' \| base64 -d > /tmp/payu-dev.kubeconfig` | — | 1 | PASSED | — |
| 579147609200 | us-east-1 | Check Nodes | `oc --kubeconfig /tmp/payu-dev.kubeconfig get nodes` | `Ready`, role `worker` | 5 | PASSED | `ip-10-0-141-218` |
| 579147609200 | us-east-1 | Check Operators | `oc --kubeconfig /tmp/payu-dev.kubeconfig get co` | All `AVAILABLE=True` | 5 | PASSED | 21 operators healthy |

### Step 7: VPC Endpoints (Security Hardening)

> Per ROSA best practices: VPC endpoints keep AWS service traffic private (no public internet). HCP CLI auto-creates S3 Gateway endpoint; we add Interface endpoints.

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Create VPCE SG | `SG_ID=$(aws ec2 create-security-group --group-name payu-dev-vpce-sg --description "VPC Endpoint SG" --vpc-id vpc-0d02f6e0681e55e2b --query 'GroupId' --output text) && aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port 443 --cidr 10.0.0.0/16` | `aws ec2 describe-security-groups --group-ids $SG_ID` | 1 | PASSED | Allow HTTPS from VPC |
| 579147609200 | us-east-1 | STS Endpoint | `aws ec2 create-vpc-endpoint --vpc-id vpc-0d02f6e0681e55e2b --service-name com.amazonaws.us-east-1.sts --vpc-endpoint-type Interface --subnet-ids subnet-086607bb7577c1092 --security-group-ids $SG_ID --private-dns-enabled` | Endpoint created | 1 | PASSED | IRSA token exchange |
| 579147609200 | us-east-1 | EC2 Endpoint | `aws ec2 create-vpc-endpoint ... --service-name com.amazonaws.us-east-1.ec2` | Endpoint created | 1 | PASSED | Cloud controller |
| 579147609200 | us-east-1 | ELB Endpoint | `aws ec2 create-vpc-endpoint ... --service-name com.amazonaws.us-east-1.elasticloadbalancing` | Endpoint created | 1 | PASSED | Ingress controller |
| 579147609200 | us-east-1 | EBS Endpoint | `aws ec2 create-vpc-endpoint ... --service-name com.amazonaws.us-east-1.ebs` | Endpoint created | 1 | PASSED | CSI driver |
| 579147609200 | us-east-1 | KMS Endpoint | `aws ec2 create-vpc-endpoint ... --service-name com.amazonaws.us-east-1.kms` | Endpoint created | 1 | PASSED | Encryption |
| 579147609200 | us-east-1 | Verify | `aws ec2 describe-vpc-endpoints --filters "Name=vpc-id,Values=vpc-0d02f6e0681e55e2b" --query 'VpcEndpoints[].[ServiceName,VpcEndpointType]' --output table` | 6 endpoints | 1 | PASSED | S3(GW) + 5 Interface |
| 579147609200 | us-east-1 | Test DNS | `oc run test --image=busybox --rm -it -- nslookup sts.us-east-1.amazonaws.com` | Private IP | 1 | PASSED | Private DNS works |

---

## 3. Post-Deployment

### 3.1 Verify Cluster

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Console URL | `oc --kubeconfig /tmp/payu-dev.kubeconfig get console.config.openshift.io cluster -o jsonpath='{.status.consoleURL}'` | `https://console-openshift-console.apps.payu-dev.sandbox2356.opentlc.com` | 1 | PASSED | — |
| 579147609200 | us-east-1 | kubeadmin Password | `oc get secret payu-dev-kubeadmin-password -n clusters -o jsonpath='{.data.password}' \| base64 -d` | `vzcte-nX5SW-42HqT-8zaM7` | 1 | PASSED | — |
| 579147609200 | us-east-1 | Switch Ingress to NLB | `oc --kubeconfig /tmp/payu-dev.kubeconfig replace -f - <<EOF'` + IngressController YAML with `aws.type: NLB` | `oc get ingresscontroller default -o jsonpath='{.spec.endpointPublishingStrategy.loadBalancer.providerParameters.aws.type}'` → `NLB` | 2 | PASSED | `oc replace` ensures old CLB is cleaned up properly |
| 579147609200 | us-east-1 | Delete old CLB | `for CLB in $(aws elb describe-load-balancers --query 'LoadBalancerDescriptions[?contains(LoadBalancerName,`<old-CLB-prefix>`)].LoadBalancerName' --output text); do aws elb delete-load-balancer --load-balancer-name $CLB; done` | No CLB with `kubernetes.io/cluster/payu-dev` tag | 1 | PASSED | Old CLBs not auto-deleted by patch |
| 579147609200 | us-east-1 | Verify NLB Active | `aws elbv2 describe-load-balancers --query 'LoadBalancers[?contains(LoadBalancerName,<prefix>)].State.Code' --output text` | `active` | 2 | PASSED | NLB target groups: ports 30905 (HTTP), 30367 (HTTPS) |
| 579147609200 | us-east-1 | Verify NLB Targets | `aws elbv2 describe-target-health --target-group-arn <TG_ARN>` | All `healthy` | 1 | PASSED | Node healthy |

### 3.2 Identity & Access (Best Practice)

> Per ROSA: use STS/OIDC for all workloads, dedicated SA per app, remove kubeadmin after IdP setup.

| Best Practice | Status | Action |
|:--------------|:-------|:-------|
| STS for all AWS access | DONE | HCP CLI uses STS |
| OIDC provider for IRSA | DONE | `oidc-storage-payu-dev` |
| Dedicated SA per workload | TODO | Apply to app deployments |
| Remove kubeadmin after IdP | TODO | Configure OIDC/LDAP IdP |
| Short-lived tokens | DONE | STS auto-rotated |

**IRSA example:**
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-app-sa
  namespace: my-app
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::579147609200:role/my-app-role
```

### 3.3 Security Hardening (Best Practice)

> Per ROSA: enforce `restricted` SCC, NetworkPolicy default-deny, least-privilege containers.

| Best Practice | Status | Action |
|:--------------|:-------|:-------|
| `restricted` SCC by default | TODO | Enforce in app namespaces |
| `allowPrivilegeEscalation: false` | TODO | Add to deployments |
| `readOnlyRootFilesystem: true` | TODO | Add to deployments |
| `runAsNonRoot: true` | TODO | Add to deployments |
| `seccompProfile: RuntimeDefault` | TODO | Add to deployments |
| Default deny NetworkPolicy | TODO | Add per namespace |
| EgressFirewall | TODO | Restrict external access |
| Compliance Operator | TODO | CIS/PCI-DSS scanning |

**Security context template:**
```yaml
spec:
  securityContext:
    runAsNonRoot: true
    seccompProfile:
      type: RuntimeDefault
  containers:
    - name: app
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities:
          drop: ["ALL"]
```

**Default deny NetworkPolicy:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: my-app
spec:
  podSelector: {}
  policyTypes: [Ingress, Egress]
```

### 3.4 Observability & Resilience (Best Practice)

| Best Practice | Status | Action |
|:--------------|:-------|:-------|
| User Workload Monitoring | TODO | Enable for app metrics |
| Cluster Logging (Loki) | TODO | Deploy Loki Operator |
| Insights Operator | DONE | Auto-enabled |
| Pod Disruption Budgets | TODO | Add for all apps |
| Topology Spread Constraints | TODO | Spread across AZs |
| Resource limits | TODO | Set requests/limits |
| cert-manager Operator | TODO | Auto-renew certs |
| External DNS Operator | TODO | Sync Route → Route53 |

### 3.5 Cost & Supply Chain (Best Practice)

| Best Practice | Status | Action |
|:--------------|:-------|:-------|
| Right-size instances | DONE | `m5.large` (2vCPU/8GB) |
| Cluster autoscaler | TODO | Production only |
| Spot instances | TODO | Add spot NodePool |
| Resource quotas | TODO | Per namespace |
| KMS for etcd encryption | TODO | `--kms-key-arn` flag (production) |
| KMS for root volume | TODO | `--root-volume-kms-key` flag |
| Image signing (Cosign) | TODO | CI/CD integration |
| External Secrets Operator | TODO | Secrets Manager |
| GitOps (ArgoCD) | TODO | Install operator |

### 3.6 Priority Matrix

| Priority | Item | Impact | Effort |
|:---------|:-----|:-------|:-------|
| P0 | NetworkPolicy default-deny | Security | Low |
| P0 | SCC restricted enforcement | Security | Low |
| P0 | Resource quotas | Cost/Stability | Low |
| P1 | cert-manager + External DNS | Operations | Medium |
| P1 | User Workload Monitoring | Observability | Low |
| P1 | PDB for all apps | Resilience | Low |
| P2 | Compliance Operator | Compliance | Medium |
| P2 | External Secrets Operator | Security | Medium |
| P2 | GitOps (ArgoCD) | Operations | Medium |
| P3 | Image signing/SBOM | Supply Chain | High |
| P3 | Spot instances | Cost | Low |

---

## 4. Destroy

| Account | Region | Activity | Details / Command / Syntax | Validation | Duration (Min) | Status | Remarks |
|:--------|:-------|:---------|:---------------------------|:-----------|:---------------|:-------|:--------|
| 579147609200 | us-east-1 | Delete NodePool | `oc delete nodepool payu-dev -n clusters` | Gone | 3 | PASSED | Terminates EC2 |
| 579147609200 | us-east-1 | Delete HostedCluster | `oc delete hostedcluster payu-dev -n clusters` | Gone | 10 | PASSED | If stuck: remove finalizers |
| 579147609200 | us-east-1 | Wait Namespace | `oc get ns clusters-payu-dev -w` | Gone | 5 | PASSED | — |
| 579147609200 | us-east-1 | Cleanup AWS | Delete VPC, IAM roles, S3, Route53, VPC endpoints | All gone | 10 | PASSED | Manual cleanup |
| 579147609200 | us-east-1 | Verify | `oc get hostedcluster -A \| grep payu-dev \|\| echo "Clean"` | No resources | 2 | PASSED | — |

---

## 5. Troubleshooting

### 5.1 General Issues

| Issue | Root Cause | Fix |
|:------|:-----------|:----|
| `VpcLimitExceeded` | Too many VPCs | Delete unused or request increase |
| `AddressLimitExceeded` | Too many EIPs | `aws ec2 release-address --allocation-id <ID>` |
| `RouteAlreadyExists` | Leftover VPC | Delete old VPC first |
| `Secret not found` | Wrong namespace | HCP CLI uses `clusters` ns |
| `latest version supported: 4.18.0` | MCE 2.8 limit | Use OCP 4.18.x |
| etcd Pending (anti-affinity) | HA needs 3 nodes | Use `SingleReplica` |
| `clusterNetwork` overlap | `10.133.0.0/14` == `10.132.0.0/14` | Use `10.136.0.0/14` |
| `WebIdentityErr` | OIDC docs missing in S3 | Re-upload JWKS with correct kid |
| Node not joining | SG missing inbound rules | Add `IpProtocol=-1, CidrIp=<VPC_CIDR>` |
| `InvalidIdentityToken` (assumed-role not authorized) | Missing `sts.amazonaws.com` audience in IAM OIDC provider. | Add `sts.amazonaws.com` client ID to OIDC provider: `aws iam add-client-id-to-open-id-connect-provider --open-id-connect-provider-arn <ARN> --client-id sts.amazonaws.com` |
| EC2 instance profile / role cannot be assumed | IAM NodePool management role trust relationship is missing `ec2.amazonaws.com`. | Add `ec2.amazonaws.com` service principal to role trust policy: `{"Effect": "Allow", "Principal": {"Service": "ec2.amazonaws.com"}, "Action": "sts:AssumeRole"}` |
| `SyncLoadBalancerFailed` (could not find suitable subnets) | Shared VPC subnets lack cluster discovery tag for the guest cluster `infraID`. | Add tags to all subnets: `Key=kubernetes.io/cluster/<infra-id>,Value=shared`. Then restart cloud-controller-manager pods to re-evaluate. |
| Console operator degraded with `no such host` | AWS VPC resolver (`10.0.0.2`) cached negative lookup (NXDOMAIN) of routes. | Patch guest DNS operator to use external upstream resolver (e.g. `8.8.8.8`) and delete CoreDNS pods to clear memory cache. |

### 5.2 Cilium CNI Issues

| Issue | Root Cause | Fix |
|:------|:-----------|:----|
| Nodes stuck `NotReady` after Cilium install | `cni.confPath` wrong (default `/etc/cni/net.d` instead of `/etc/kubernetes/cni/net.d`) | `helm upgrade cilium cilium/cilium -n kube-system --set cni.confPath=/etc/kubernetes/cni/net.d` |
| Console `RouteHealthDegraded: i/o timeout` | CoreDNS using AWS VPC resolver (`10.0.0.2`) which cached NXDOMAIN for `*.apps` | Patch DNS operator: `upstreamResolvers.upstreams[0] = {type: Network, address: 8.8.8.8}` |
| Console `RouteHealthDegraded: context deadline exceeded` | Worker SG blocks NLB health checks (NLB preserves client IP) | Add SG ingress rule: `IpProtocol=-1, CidrIp=10.0.0.0/16` |
| `api.payu-dev` NXDOMAIN from within VPC | CNAME only in public zone, not in private zone | Add CNAME to private hosted zone |
| Console not accessible from laptop/internet | Ingress NLB scope is `Internal` (private) | Patch IngressController: `scope: External`, then delete `router-default` svc to force NLB recreation |
| NLB targets stuck `initial` (registration in progress) | NLB just provisioned | Wait 2-3 minutes for health checks to complete |
| `networkType: Cilium` not recognized | HyperShift does not support `Cilium` as networkType | Use `networkType: Other` instead |

---

## 6. Key Files & References

| File | Purpose |
|:-----|:--------|
| `iam/trust.json` | IAM trust policy — `sts:AssumeRole` for student user |
| `iam/policy.json` | IAM permissions — EC2, ELB, IAM, Route53, S3 |
| `manifests/hostedcluster-payu.yaml` | HostedCluster CR |
| `manifests/nodepools-payu.yaml` | NodePool CR |

| Document | URL |
|:---------|:----|
| OCP 4.18 HCP Docs | https://docs.redhat.com/en/documentation/openshift_container_platform/4.18/html/hosted_control_planes/ |
| ROSA Best Practices | https://cloud.redhat.com/experts/rosa/best-practices-recommendations/ |
| HyperShift GitHub | https://github.com/openshift/hypershift |
