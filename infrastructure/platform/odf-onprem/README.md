# ODF AWS STS Setup on payu-onprem

This directory contains the configurations required to deploy OpenShift Data Foundation (ODF) on the **payu-onprem** cluster using AWS STS (IAM Roles for Service Accounts) and restricting Ceph services to worker nodes labeled with `node-role.kubernetes.io/odf: ""`.

---

## AWS IAM Setup

The ODF operator needs an AWS IAM Role to manage S3 buckets for the Multicloud Object Gateway (NooBaa). Follow these steps using the AWS CLI:

### 1. Create the IAM Policy
Use the [iam-policy.json](iam-policy.json) file to create the policy:
```bash
aws iam create-policy \
  --policy-name payu-onprem-odf-noobaa-policy \
  --policy-document file://iam-policy.json
```
*Note: Make a note of the Policy ARN returned (e.g., `arn:aws:iam::787842753050:policy/payu-onprem-odf-noobaa-policy`).*

### 2. Create the IAM Role with OIDC Trust Relationship
Use the [iam-trust-policy.json](iam-trust-policy.json) file (which specifies the trust relationship to the `payu-onprem` cluster OIDC Provider) to create the role:
```bash
aws iam create-role \
  --role-name payu-onprem-odf-noobaa \
  --assume-role-policy-document file://iam-trust-policy.json
```

### 3. Attach the IAM Policy to the Role
```bash
aws iam attach-role-policy \
  --role-name payu-onprem-odf-noobaa \
  --policy-arn arn:aws:iam::787842753050:policy/payu-onprem-odf-noobaa-policy
```

---

## Kubernetes Deployment Steps

### 1. Label the Designated ODF Nodes
Label the nodes where ODF services should run:
```bash
oc --kubeconfig ~/.kube/payu-onprem.kubeconfig label node ip-10-0-59-207.ec2.internal node-role.kubernetes.io/odf=""
```

### 2. Apply ODF Operator Manifest
Install the namespace, OperatorGroup, and Subscription for the ODF Operator:
```bash
oc --kubeconfig ~/.kube/payu-onprem.kubeconfig apply -f odf-operator.yaml
```
Verify the Subscription has resolved and CSV transitions to `Succeeded`:
```bash
oc --kubeconfig ~/.kube/payu-onprem.kubeconfig get csv -n openshift-storage
```

### 3. Apply the STS Secret
Deploy the credentials secret containing the target role ARN for NooBaa:
```bash
oc --kubeconfig ~/.kube/payu-onprem.kubeconfig apply -f noobaa-secret.yaml
```

### 4. Create the ODF StorageCluster
Deploy the StorageCluster manifest:
```bash
oc --kubeconfig ~/.kube/payu-onprem.kubeconfig apply -f storagecluster.yaml
```
Verify Ceph cluster pods status (Ceph-OSD, Ceph-MGR, Ceph-MON, NooBaa) run on the labeled worker node:
```bash
oc --kubeconfig ~/.kube/payu-onprem.kubeconfig get pods -n openshift-storage -o wide
```
