#!/bin/bash
# Generate HostedCluster + NodePool YAMLs from Terraform outputs
set -euo pipefail

TF_OUTPUTS=/tmp/tf-outputs.json
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFESTS_DIR="${SCRIPT_DIR}/../manifests"
mkdir -p "$MANIFESTS_DIR"

# Refresh outputs JSON
cd "${SCRIPT_DIR}/../terraform"
terraform output -json > $TF_OUTPUTS

SHARED_BUCKET=$(jq -r '.shared_oidc_bucket.value' $TF_OUTPUTS)
SHARED_BUCKET_HOST="${SHARED_BUCKET}.s3.ap-southeast-1.amazonaws.com"

for KEY in payu-onprem payu-cloud; do
  echo "=== Generating manifests for $KEY ==="

  VPC=$(jq -r --arg k $KEY '.cluster_vpcs.value[$k]' $TF_OUTPUTS)
  SUBNET=$(jq -r --arg k $KEY '.cluster_public_subnet_ids.value[$k][0]' $TF_OUTPUTS)
  PROFILE=$(jq -r --arg k $KEY '.cluster_worker_instance_profiles.value[$k]' $TF_OUTPUTS)
  OIDC_URL="https://${SHARED_BUCKET_HOST}/${KEY}"
  CPO=$(jq -r --arg k $KEY '.cluster_ids.value[$k].control_plane_operator' $TF_OUTPUTS)
  REG=$(jq -r --arg k $KEY '.cluster_ids.value[$k].openshift_image_registry' $TF_OUTPUTS)
  ING=$(jq -r --arg k $KEY '.cluster_ids.value[$k].openshift_ingress' $TF_OUTPUTS)
  CCC=$(jq -r --arg k $KEY '.cluster_ids.value[$k].cloud_controller' $TF_OUTPUTS)
  CNCC=$(jq -r --arg k $KEY '.cluster_ids.value[$k].cloud_network_config_controller' $TF_OUTPUTS)
  NP=$(jq -r --arg k $KEY '.cluster_ids.value[$k].node_pool' $TF_OUTPUTS)
  EBS=$(jq -r --arg k $KEY '.cluster_ids.value[$k].aws_ebs_csi_driver_controller' $TF_OUTPUTS)

  if [ "$KEY" = "payu-onprem" ]; then
    OCP_VERSION="4.15.59-multi"
    CHANNEL="stable-4.15"
    ENV="onprem"
    CLUSTER_CIDR="10.132.0.0/14"
    SERVICE_CIDR="172.31.0.0/16"
  else
    OCP_VERSION="4.20.24-multi"
    CHANNEL="stable-4.20"
    ENV="cloud"
    CLUSTER_CIDR="10.136.0.0/14"
    SERVICE_CIDR="172.32.0.0/16"
  fi

  ZONE="ap-southeast-1a"
  INSTANCE_TYPE="m6a.4xlarge"
  REPLICAS=2
  ROOT_VOL=120

  cat > $MANIFESTS_DIR/hostedcluster-$KEY.yaml <<YAML
apiVersion: hypershift.openshift.io/v1beta1
kind: HostedCluster
metadata:
  name: $KEY
  namespace: clusters
  labels:
    app.kubernetes.io/part-of: payu
    environment: $ENV
spec:
  channel: $CHANNEL
  release:
    image: quay.io/openshift-release-dev/ocp-release:$OCP_VERSION
  dns:
    baseDomain: payu.ocp.fajjjar.my.id
    privateZoneID: Z0688851VIBKG68U8DFU
    publicZoneID: Z0716734HV77ZJQGV03V
  platform:
    aws:
      region: ap-southeast-1
      endpointAccess: Public
      multiArch: false
      cloudProviderConfig:
        subnet:
          id: $SUBNET
        vpc: $VPC
        zone: $ZONE
      rolesRef:
        controlPlaneOperatorARN: $CPO
        imageRegistryARN: $REG
        ingressARN: $ING
        kubeCloudControllerARN: $CCC
        networkARN: $CNCC
        nodePoolManagementARN: $NP
        storageARN: $EBS
      resourceTags:
        - key: kubernetes.io/cluster/$KEY
          value: owned
        - key: app.kubernetes.io/part-of
          value: payu
        - key: environment
          value: $ENV
        - key: cost-center
          value: platform-engineering
        - key: owner
          value: payu-team
    type: AWS
  networking:
    clusterNetwork:
      - cidr: $CLUSTER_CIDR
    machineNetwork:
      - cidr: 10.0.0.0/16
    networkType: OVNKubernetes
    serviceNetwork:
      - cidr: $SERVICE_CIDR
  infraID: $KEY
  fips: false
  issuerURL: $OIDC_URL
  configuration:
    ingress:
      loadBalancer:
        platform:
          aws:
            type: NLB
  capabilities: {}
  olmCatalogPlacement: management
  services:
    - service: APIServer
      servicePublishingStrategy:
        type: LoadBalancer
    - service: OAuthServer
      servicePublishingStrategy:
        type: Route
    - service: Konnectivity
      servicePublishingStrategy:
        type: Route
    - service: Ignition
      servicePublishingStrategy:
        type: Route
  controllerAvailabilityPolicy: SingleReplica
  infrastructureAvailabilityPolicy: SingleReplica
  autoscaling:
    scaling: ScaleUpAndScaleDown
  etcd:
    managed:
      storage:
        persistentVolume:
          size: 8Gi
          storageClassName: gp3-csi
        type: PersistentVolume
    managementType: Managed
  secretEncryption:
    aescbc:
      activeKey:
        name: $KEY-etcd-encryption-key
    type: aescbc
  pullSecret:
    name: $KEY-pull-secret
YAML

  cat > $MANIFESTS_DIR/nodepools-$KEY.yaml <<YAML
apiVersion: hypershift.openshift.io/v1beta1
kind: NodePool
metadata:
  name: $KEY
  namespace: clusters
  labels:
    hypershift.openshift.io/cluster: $KEY
    app.kubernetes.io/part-of: payu
spec:
  clusterName: $KEY
  arch: amd64
  release:
    image: quay.io/openshift-release-dev/ocp-release:$OCP_VERSION
  replicas: $REPLICAS
  management:
    autoRepair: false
    upgradeType: Replace
  platform:
    aws:
      instanceType: $INSTANCE_TYPE
      instanceProfile: $PROFILE
      rootVolume:
        size: $ROOT_VOL
        type: gp3
      subnet:
        id: $SUBNET
      resourceTags:
        - key: kubernetes.io/cluster/$KEY
          value: owned
        - key: app.kubernetes.io/part-of
          value: payu
        - key: environment
          value: $ENV
        - key: cost-center
          value: platform-engineering
        - key: owner
          value: payu-team
    type: AWS
YAML
  echo "  ✓ Generated: hostedcluster-$KEY.yaml + nodepools-$KEY.yaml"
done
echo
echo "=== Verify shared bucket URL in generated manifests ==="
grep "issuerURL" $MANIFESTS_DIR/hostedcluster-payu-onprem.yaml
grep "issuerURL" $MANIFESTS_DIR/hostedcluster-payu-cloud.yaml
