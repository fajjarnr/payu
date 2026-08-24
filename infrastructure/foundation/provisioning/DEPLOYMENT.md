# PayU OpenShift IPI — `payu` AWS STS Deployment Guide

> **Deployment Mode**: Installer-Provisioned Infrastructure (IPI) on AWS (Existing VPC/Subnets)
> **Credentials Mode**: AWS STS (Security Token Service) via Manual Mode
> **Platform**: AWS ap-southeast-1 (Singapore)
> **OCP Version**: 4.22+
> **CNI**: OVN-Kubernetes (default)
> **Last Updated**: 2026-06-11
> **References**: [OCP 4.22 AWS STS Installation Docs](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/authentication_and_authorization/manually-creating-iam-roles-for-aws) | [Cloud Credential Operator & ccoctl](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/authentication_and_authorization/preparing-to-install-with-sts)

---

## 1. Concept Overview

Dalam instalasi OpenShift standar pada AWS (IPI/UPI), **Cloud Credential Operator (CCO)** secara default berjalan dalam mode **Mint** (di mana cluster memegang Access Key administrator AWS jangka panjang dan secara dinamis membuat user/role baru untuk komponen cluster).

Untuk arsitektur perbankan enterprise **PayU** yang aman dan mematuhi regulasi Zero-Trust & PCI-DSS, kita menggunakan **AWS STS (Security Token Service) Manual Mode**:
1. **No Long-term Secrets**: Tidak ada AWS Access Key/Secret Key yang disimpan di dalam cluster OpenShift.
2. **Short-lived Tokens**: Komponen internal OpenShift (seperti Ingress, CSI Driver, Registry) menggunakan token JWT Kubernetes (ServiceAccount token) yang ditukarkan dengan token STS berumur pendek melalui **AWS IAM Roles for Service Accounts (IRSA)**.
3. **Decentralized OIDC Provider**: Kunci verifikasi divalidasi oleh AWS melalui OpenID Connect (OIDC) Discovery Endpoint yang di-host di S3 bucket publik.

---

## 2. Pre-requisites

### 2.1 Tooling & Environment

| Tool / Resource | Required Version | Purpose / Command | Status |
|:----------------|:-----------------|:------------------|:-------|
| **AWS CLI** | `2.x+` | Konfigurasi awal & AWS API communication (`aws configure`) | PENDING |
| **AWS IAM Permissions** | `AdministratorAccess` | Dibutuhkan untuk menjalankan CLI `ccoctl` yang membuat IAM Policy/Roles, OIDC Provider, dan S3 Bucket | PENDING |
| **openshift-install** | `4.22.x` | Installer CLI untuk men-deploy cluster | PENDING |
| **oc CLI** | `4.22.x` | Client CLI untuk berinteraksi dengan cluster & mengekstrak resource dari release image | PENDING |
| **Pull Secret** | Valid Red Hat pull secret | Mengunduh container image resmi dari Red Hat Registry | PENDING |
| **SSH Key** | `ssh-rsa` or `ed25519` | Akses debugging SSH ke node RHCOS jika diperlukan | PENDING |

### 2.2 Network Setup (Existing VPC & Subnets)
Sesuai konfigurasi di [install-config.yaml](file:///Users/fajar/Projects/sumbermakmur/payu/infrastructure/foundation/provisioning/install-config.yaml), cluster di-deploy menggunakan subnets yang sudah ada di region `ap-southeast-1`:

| Subnet ID | Availability Zone | Type | CIDR Range (Contoh) |
|:----------|:------------------|:-----|:--------------------|
| `subnet-0b7a5d88e3ca54d83` | ap-southeast-1a | Public (Ingress/ELB) | `10.0.1.0/24` |
| `subnet-0f77f63cad500cfa4` | ap-southeast-1b | Public (Ingress/ELB) | `10.0.2.0/24` |
| `subnet-0d4fce9c46243d294` | ap-southeast-1c | Public (Ingress/ELB) | `10.0.3.0/24` |
| `subnet-0a3ab903e9b914976` | ap-southeast-1a | Private (Master/Worker) | `10.0.11.0/24` |
| `subnet-05252b7e295378733` | ap-southeast-1b | Private (Master/Worker) | `10.0.12.0/24` |
| `subnet-092dd4ff46a20cb20` | ap-southeast-1c | Private (Master/Worker) | `10.0.13.0/24` |

---

## 3. Deployment Steps

Proses deployment dibagi menjadi tiga tahap utama: Ekstraksi target credentials, pembuatan AWS IAM & OIDC via `ccoctl`, serta deployment cluster via `openshift-install`.

### Step 1: Ekstraksi `ccoctl` dan Credentials Requests

Ekstrak utilitas `ccoctl` dan `CredentialsRequest` objek dari release image resmi OpenShift.

| Activity | Command / Syntax | Validation | Status |
|:---------|:-----------------|:-----------|:-------|
| 1. Dapatkan Release Image | `RELEASE_IMAGE=$(openshift-install version \| awk '/release image/ {print $3}')` | `$RELEASE_IMAGE` tidak kosong | PENDING |
| 2. Ekstrak `ccoctl` Tool | `oc adm release extract --tools $RELEASE_IMAGE --to=./temp-tools` | File `ccoctl` hasil ekstrak ada | PENDING |
| 3. Pasang `ccoctl` | `tar -xvf ./temp-tools/ccoctl-mac.tar.gz -C . && chmod +x ./ccoctl` | `./ccoctl --help` berhasil | PENDING |
| 4. Ekstrak Requests | `oc adm release extract --credentials-requests --to=./credrequests $RELEASE_IMAGE` | Folder `./credrequests` terisi file YAML | PENDING |

---

### Step 2: Buat S3 Bucket, OIDC Provider, dan IAM Roles

Jalankan `ccoctl` menggunakan kredensial administrator AWS lokal Anda. Tool ini akan otomatis memanggil AWS API untuk menyiapkan infrastruktur autentikasi token.

```bash
./ccoctl aws-sts create-all \
  --name=payu \
  --region=ap-southeast-1 \
  --credentials-requests-dir=./credrequests \
  --output-dir=./ccoctl-output
```

| Created AWS Resource | Description | Purpose | Validation |
|:---------------------|:------------|:--------|:-----------|
| **S3 Bucket** | `payu-oidc-*` | Menyimpan dokumen publik OpenID Configuration & Keys (`jwks.json`) | `aws s3 ls` |
| **OIDC Provider** | IAM OIDC Identity Provider | Menghubungkan trust relationship antara AWS IAM dengan Kubernetes API cluster | AWS IAM Console |
| **IAM Policies & Roles** | Roles (e.g., `payu-openshift-image-registry-*`) | Role spesifik untuk operator seperti Ingress, CSI Driver, Cert Manager, Image Registry | AWS IAM Roles List |

*Di folder lokal `./ccoctl-output/manifests/`, tool akan menghasilkan file-file Kubernetes Secret yang berisi parameter ARN IAM Role masing-masing operator.*

---

### Step 3: Modifikasi `install-config.yaml`

Pastikan file [install-config.yaml](file:///Users/fajar/Projects/sumbermakmur/payu/infrastructure/foundation/provisioning/install-config.yaml) Anda sudah dikonfigurasi untuk menggunakan mode `Manual`.

```yaml
apiVersion: v1
baseDomain: ocp.fajjjar.my.id
credentialsMode: Manual  # <-- CRITICAL: Mengaktifkan mode STS
metadata:
  name: payu
platform:
  aws:
    region: ap-southeast-1
...
```

---

### Step 4: Integrasi Manifests & Trigger Installation

Satukan manifests dasar cluster dengan manifests IAM Role hasil buatan `ccoctl`.

| Phase | Command / Action | Validation / Expected Output | Status |
|:------|:-----------------|:-----------------------------|:-------|
| 1. Create Workspace | `mkdir -p ./workspace` | Direktori `workspace` siap | PENDING |
| 2. Copy Config | `cp install-config.yaml ./workspace/` | File tersalin ke `./workspace/` | PENDING |
| 3. Generate Manifests | `openshift-install create manifests --dir=./workspace` | Folder `./workspace/manifests/` terbuat | PENDING |
| 4. Inject STS Manifests | `cp ./ccoctl-output/manifests/* ./workspace/manifests/` | File `*credentials.yaml` tersalin ke folder manifests | PENDING |
| 5. Inject TLS Keys | `cp -R ./ccoctl-output/tls ./workspace/` | Folder `./workspace/tls` berisi file key pair | PENDING |
| 6. Deploy Cluster | `openshift-install create cluster --dir=./workspace` | Proses instalasi berjalan hingga selesai (~30-40 menit) | PENDING |

---

## 4. Post-Deployment Verification

Setelah instalasi selesai, verifikasi status cluster untuk memastikan STS/OIDC berjalan dengan baik.

### 4.1 Cluster Health Check

| Command | Expected Output | Status |
|:--------|:----------------|:-------|
| `oc get nodes` | Semua nodes (3 master, 3 worker) berstatus `Ready` | PENDING |
| `oc get co` | Seluruh cluster operators memiliki status `AVAILABLE=True` & `DEGRADED=False` | PENDING |
| `oc get clusteroperator authentication` | `AVAILABLE=True`, `DEGRADED=False` | PENDING |

### 4.2 CCO Verification (Cloud Credential Operator)

Pastikan CCO mendeteksi bahwa mode autentikasi berjalan secara manual dengan STS:

```bash
oc get cloudcredential cluster -o jsonpath='{.spec.credentialsMode}'
```
*Output yang benar: **`Manual`***

Verifikasi status sinkronisasi rahasia operator:
```bash
oc get credentialsrequest -A
```
*Semua `CredentialsRequest` harus memiliki status `PROVISIONED`.*

### 4.3 Contoh Workload menggunakan STS (IRSA)

Setiap aplikasi internal PayU yang memerlukan akses AWS resources (seperti S3 Bucket atau DynamoDB) wajib menggunakan ServiceAccount yang dianotasi dengan IAM Role ARN.

Contoh manifest deployment aplikasi:
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: payu-ledger-s3-sa
  namespace: payu-ledger
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/payu-ledger-s3-role
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payu-ledger
  namespace: payu-ledger
spec:
  replicas: 2
  template:
    spec:
      serviceAccountName: payu-ledger-s3-sa
      containers:
        - name: ledger-app
          image: quay.io/payu/ledger:v1.0.0
```

---

## 5. Destroy Cluster

Untuk menghapus cluster dan seluruh infrastruktur AWS (termasuk role IAM STS) dengan bersih:

```bash
# 1. Hapus resource AWS utama & instance EC2 via Installer
openshift-install destroy cluster --dir=./workspace

# 2. Hapus OIDC Provider, S3 OIDC bucket, dan IAM Roles yang dibuat ccoctl
./ccoctl aws-sts delete \
  --name=payu \
  --region=ap-southeast-1 \
  --credentials-requests-dir=./credrequests
```

---

## 6. Troubleshooting

| Issue | Root Cause | Fix / Solution |
|:------|:-----------|:---------------|
| `WebIdentityErr: failed to retrieve credentials` | Trust relationship di IAM Role tidak mengenali OIDC issuer URL. | 1. Cek S3 bucket OIDC Anda, pastikan berstatus publik agar AWS STS API dapat mengambil OIDC keys.<br>2. Verifikasi thumbprint SSL pada OIDC provider di AWS Console. |
| `openshift-install` stuck saat bootstrap | Operator tidak dapat mengakses AWS API karena STS IAM Role tidak di-inject dengan benar ke manifest. | Pastikan Anda telah menyalin seluruh file manifest dari `./ccoctl-output/manifests/*` ke `./workspace/manifests/` **sebelum** menjalankan `create cluster`. |
| AWS API Rate Limit (Throttling) | Terlalu banyak request secara simultan selama proses `ccoctl` atau pembuatan node. | Tunggu beberapa menit, `openshift-install` secara otomatis memiliki retry backoff logic. Jika diperlukan, jalankan ulang command instalasi. |
| `InvalidParameter: OIDC Provider already exists` | OIDC provider dari instalasi sebelumnya belum dihapus secara bersih. | Hapus OIDC provider lama di AWS Console (IAM -> Identity Providers) sebelum menjalankan ulang `ccoctl aws-sts create-all`. |
