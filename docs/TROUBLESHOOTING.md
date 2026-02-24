# PayU Troubleshooting Guide

> **Common issues and solutions for PayU Digital Banking Platform development and operations.**

---

## 📋 Table of Contents

1. [Quick Diagnosis Flowchart](#1-quick-diagnosis-flowchart)
2. [Installation & Build Issues](#2-installation--build-issues)
3. [Container & Orchestration Issues](#3-container--orchestration-issues)
4. [Database & State Issues](#4-database--state-issues)
5. [Frontend & Backend Communication](#5-frontend--backend-communication)
6. [OpenShift-Specific Issues (Production/SIT)](#6-openshift-specific-issues)
7. [Performance & Resource Tuning](#7-performance--resource-tuning)
8. [Known PayU Platform Issues](#8-known-payu-platform-issues)
9. [Quick Reference Commands](#9-quick-reference-commands)

---

## 1. Quick Diagnosis Flowchart

Gunakan diagram ini untuk identifikasi awal masalah container/pod:

```
Container/Pod tidak jalan?
├── Exit Code 137 (OOM Killed)?
│   └── → Lihat bagian Resource Tuning
├── Status CrashLoopBackOff?
│   ├── Exit 1 segera? → Salah Config / Missing Env
│   └── Restat setelah beberapa saat? → Health Check / Readiness Probe gagal
├── Status ImagePullBackOff?
│   └── → Cek Registry / ImageStream / Tag
└── Status Pending?
    └── → Resource Quota penuh atau Scheduling issues
```

---

## 2. Installation & Build Issues

### Setup Script Fails
**Problem**: `./setup.sh` gagal dijalankan.
**Solutions**:
- Cek kompatibilitas OS: `cat /etc/os-release`.
- Jalankan dengan verbose: `bash -x ./scripts/setup.sh 2>&1 | tee setup.log`.
- Pastikan script executable: `chmod +x scripts/*.sh`.

### Maven Build Fails (Cannot find symbol)
**Problem**: Gagal build karena `api-commons` atau `security-starter` tidak ditemukan.
**Solutions**:
- Build shared libraries terlebih dahulu: `cd backend/shared && mvn clean install -DskipTests`.
- Jika masalah Lombok: Pastikan annotation processor aktif di IDE.

### Frontend Build Issues
**Problem**: `npm install` gagal atau Node version mismatch.
**Solutions**:
- Pastikan Node.js v22+. Gunakan `nvm use 22`.
- Bersihkan cache: `rm -rf node_modules package-lock.json && npm install --legacy-peer-deps`.

---

## 3. Container & Orchestration Issues

### Exit Code 137 (OOM Killed)
**Symptoms**: Container mati tiba-tiba dengan code 137.
**Diagnosis**:
- Podman: `podman inspect <container> | grep -i oom`.
- OpenShift: `oc describe pod <pod> | grep -A5 "Reason:"`.
**Fix**: Naikkan `mem_limit` di `podman-compose.yml` atau resources di Deployment manifest.

### Health Check (Readiness Probe) Gagal
**Symptoms**: `/actuator/health` return `DOWN`.
**Solutions**:
- Framework Standard:
  - Spring: `/actuator/health/liveness` & `/actuator/health/readiness`
  - Quarkus: `/q/health/live` & `/q/health/ready`
  - Python: `/health`
- Cek log servis untuk melihat komponen mana yang DOWN (DB, Redis, atau Kafka).

---

## 4. Database & State Issues

### PostgreSQL Connection Refused
**Problem**: Servis tidak bisa konek ke Postgres.
**Solutions**:
- Cek status: `podman ps | grep postgres`.
- Cek kesiapan: `podman exec payu-postgres pg_isready -U payu`.
- Restart: `podman restart payu-postgres`.
- Database Name: Pastikan DB `payu_[service_name]` sudah ada.

### Flyway Checksum Mismatch
**Problem**: Gagal migrasi karena checksum file SQL berubah.
**Fix (Dev)**: Reset DB `dropdb -h localhost -U payu payu_account && createdb -h localhost -U payu payu_account`.
**Fix (Prod)**: Update manual tabel `flyway_schema_history` atau jalankan `mvn flyway:repair`.

### Keycloak DB Issues
**Problem**: Keycloak gagal start karena DB belum siap.
**Solutions**: Tunggu Postgres benar-benar sehat sebelum start Keycloak. Jika data korup di lokal, hapus volume: `podman volume rm payu_keycloak_data`.

---

## 5. Frontend & Backend Communication

### API Calls Failing (CORS)
**Problem**: Browser memblokir request ke backend.
**Solutions**: Cek konfigurasi CORS di `gateway-service` (application.yml). Pastikan `allowed-origins: ["http://localhost:3001"]` (atau URL env yang sesuai).

### Web App Blank Page
**Solutions**:
- Cek console browser (F12).
- Pastikan `NEXT_PUBLIC_API_URL` mengarah ke gateway yang benar.
- Clear cache Next.js: `rm -rf .next` di folder web-app.

---

## 6. OpenShift-Specific Issues

### Permission Denied (User ID Acak)
**Root Cause**: OpenShift menjalankan container dengan randomly assigned non-root user ID.
**Fix**: Pastikan direktori `/deployments` atau folder logs memiliki permission group writable (`chmod -R g=u`).

### NetworkPolicy Blocking
**Symptoms**: Servis dalam cluster tidak bisa saling panggil.
**Fix**: Pastikan Pod memiliki label yang benar (e.g., `app.kubernetes.io/part-of: payu-banking`) agar sesuai dengan ingress selector NetworkPolicy.

### ConfigMap/Secret Tidak Terdeteksi
**Fix**: Verifikasi nama ConfigMap di YAML tepat sama dengan yang ada di oc (case sensitive). Restart pod setelah update ConfigMap.

---

## 7. Performance & Resource Tuning

### Service Startup Lambat
**Solutions**: Matikan readiness state health check saat startup jika dirasa memperlambat: `management.health.readinessstate.enabled=false`. Naikkan `start_period` di healthcheck compose.

### Kebutuhan RAM PayU (Rekomendasi)
| Servis | Min RAM | Recommended |
|:-------|:--------|:------------|
| Java (Standard) | 512MB | 1GB |
| Quarkus Native | 256MB | 512MB |
| ML (KYC/Analytics) | 1GB | 2GB |
| Keycloak/Kafka | 2GB | 4GB |

---

## 8. Known PayU Platform Issues

| Masalah | Gejala | Solusi Cepat |
|:---|:---|:---|
| **Keycloak DNS** | Eksternal Postgres gagal | Gunakan FQDN lengkap untuk host DB. |
| **DataGrid RESP** | Gateway koneksi tertutup | (Dev) Set `endpointEncryption.type: None`. |
| **Spring Context Path** | 404 pada semua API | Cek `server.servlet.context-path` sudah sesuai. |
| **Quarkus Profile** | Config tidak load | Gunakan `QUARKUS_PROFILE=prod` saat deploy. |

---

## 9. Quick Reference Commands

### Podman Compose (Lokal)
```bash
# Start total (clean)
podman-compose down -v && podman-compose up -d

# Cek logs stream
podman logs -f payu-account-service

# Masuk ke container shell
podman exec -it payu-account-service /bin/bash
```

### OpenShift (Cloud/SIT)
```bash
# Cek pod dan event terbaru
oc get pods -n payu-dev
oc get events -n payu-dev --sort-by='.lastTimestamp'

# Log instance sebelumnya (setelah crash)
oc logs <pod-name> -p

# Port Forwarding untuk testing lokal
oc port-forward pod/<pod-name> 8080:8080
```

---

## 🆘 Butuh bantuan lebih lanjut?
1. Cek **[docs/guides/LESSONS.md](./guides/LESSONS.md)** untuk pola teknis mendalam.
2. Cek **docs/roadmap/TODOS.md** untuk daftar bug yang sudah diketahui.
3. Laporkan ke channel `#payu-dev` dengan menyertakan: OS, Tool version, Error log, dan langkah reproduksi.

---
_Last Updated: February 24, 2026_
