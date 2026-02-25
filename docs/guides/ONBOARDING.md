# PayU Developer Onboarding Guide

> **Your guide to first-day setup, environment configuration, and development workflow.**

Selamat datang di PayU Digital Banking Platform! Panduan ini akan membantumu menyiapkan lingkungan pengembangan serta memahami struktur dan alur kerja platform kami.

---

## 📋 Pre-Onboarding Checklist

Pastikan hal-hal berikut sudah siap atau dalam proses sebelum memulai:

- [ ] **Laptop Perusahaan** dengan akses admin.
- [ ] **Akun GitHub** yang sudah ditambahkan ke organisasi PayU.
- [ ] **Akses Cluster OpenShift** (untuk stage dev/staging).
- [ ] **Workspace Slack** (gabung ke channel `#payu-dev`).
- [ ] **Akses Email Korporat**.

---

## 🛠️ 1. Prerequisites (Kebutuhan Sistem)

Pastikan *tooling* berikut terinstall di mesin lokalmu. Gunakan script verifikasi jika ragu.

| Tool | Version | Description |
|:-----|:--------|:------------|
| **Java** | 21+ LTS | Runtime inti untuk backend (Spring Boot & Quarkus). |
| **Maven** | 3.9+ | Build tool untuk ekosistem Java. |
| **Node.js** | 22+ LTS | Runtime untuk frontend (Next.js & React Native). |
| **Python** | 3.12+ | Runtime untuk servis AI/ML (FastAPI). |
| **Podman** | Latest | Container runtime utama (rootless). Docker bisa jadi fallback. |
| **Git** | Latest | Akses repositori dan version control. |

### Verifikasi Cepat
```bash
# Jalankan script verifikasi dependensi
./scripts/verify-env.sh --deps
```

Jika ada yang kurang, kamu bisa menjalankan script setup otomatis (mendukung Ubuntu, Fedora, macOS):
```bash
./scripts/setup.sh
```

---

## 🚀 2. Day 1: Setup Lingkungan (Quick Start)

Langkah awal untuk menjalankan PayU di mesin lokalmu.

### 2.1 Clone Repositori
```bash
# Clone dengan SSH (direkomendasikan)
git clone git@github.com:payu-id/payu.git
cd payu

# Atau HTTPS
git clone https://github.com/payu-id/payu.git
cd payu
```

### 2.2 Konfigurasi Git
```bash
git config --global user.name "Nama Kamu"
git config --global user.email "email.kamu@payu.fajjjar.my.id"
git config --global init.defaultBranch main
```

### 2.3 Menjalankan Seluruh Platform
Cara termudah untuk memulai adalah menggunakan script `setup-dev.sh`:
```bash
# Clone, build, dan jalankan seluruh servis infrasruktur + backend
./scripts/setup-dev.sh
```
Script ini akan:
1. Verifikasi dependensi.
2. Build shared starters (libraries).
3. Build semua microservices Java & Python.
4. Menjalankan infrastruktur (Postgres, Redis, Kafka, Keycloak) via Podman Compose.
5. Verifikasi kesehatan (*health check*) semua servis.

### 2.4 Setup Manual (Opsional)
Jika kamu ingin kontrol lebih detail:
```bash
# 1. Build shared libraries
cd backend/shared && mvn clean install -DskipTests

# 2. Start Infrastruktur
cd ../.. && podman compose up -d postgres redis kafka keycloak

# 3. Jalankan service spesifik
cd backend/account-service && mvn spring-boot:run
```

---

## 💻 3. IDE & Editor Configuration

Kami merekomendasikan **VS Code** atau **IntelliJ IDEA**.

### VS Code Extension pack:
```bash
code --install-extension redhat.java
code --install-extension vscjava.vscode-java-pack
code --install-extension ms-python.python
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
```

### IntelliJ IDEA:
- Install **Spring Boot** & **Lombok** plugins.
- Aktifkan **Annotation Processing** di settings.

---

## 🏗️ 4. Project Structure (Struktur Repositori)

```
payu/
├── backend/                    # Implementasi Microservices
│   ├── shared/                 # Libraries/Starter yang digunakan bersama
│   ├── [service-name]/         # Microservice spesifik (Java/Quarkus)
│   ├── kyc-service/            # OCR & Liveness (Python/FastAPI)
│   └── simulators/             # Mock eksternal (BI-FAST, etc.)
├── frontend/                   # Aplikasi Web dan Mobile
│   ├── web-app/                # Digital Banking UI (Next.js)
│   ├── mobile/                 # Mobile Application (React Native)
│   └── developer-docs/         # Partner Portal
├── docs/                       # Dokumentasi (C4, PRD, Guides)
├── infrastructure/             # Manifest OpenShift & K8s
├── scripts/                    # Script automasi (setup, test, deploy)
└── .agent/                     # AI Skills & Agent Ecosystem
```

---

## 🏃 5. Running & Accessing Services

Setelah menjalankan `./scripts/setup-dev.sh`, berikut adalah endpoint penting:

| Service | URL | Description |
|:--------|:-----|:------------|
| **Web App** | http://localhost:3001 | Main Consumer Interface |
| **API Gateway** | http://localhost:8080 | Backend Entry Point |
| **API Portal** | http://localhost:8080/api-docs | Interactive Swagger UI |
| **Keycloak Admin**| http://localhost:8099 | Identity Management (master realm) |
| **Jaeger UI** | http://localhost:16686 | Distributed Tracing |

### Kredensial Default (Local)
- **Keycloak Admin**: `admin` / `P@ssw0rd123`
- **Customer Account**: `customer1` / `P@ssw0rd123`

---

## 🔄 6. Development Workflow (Alur Kerja)

### Backend (Java)
1. Edit kode di `backend/[service]`.
2. Build: `mvn clean package -DskipTests`.
3. Restart container: `podman restart payu-[service]`.
4. Format kode: `mvn spotless:apply`.

### Frontend (Next.js)
1. `cd frontend/web-app`.
2. `npm install`.
3. `npm run dev`.

### Python (KYC/Analytics)
1. Gunakan venv (`python3 -m venv .venv`).
2. `pip install -r requirements.txt`.
3. Jalankan: `uvicorn app.main:app --reload`.

---

## 🧪 7. Testing (Standa Pengujian)

PayU mengadopsi TDD (*Test Driven Development*).

- **Unit Tests**: `mvn test` (backend) atau `npm test` (frontend).
- **Integration Tests**: Gunakan profile `integration` atau Testcontainers.
- **E2E Tests**: Jalankan `npx playwright test` di folder `frontend/web-app`.

---

## 🆘 8. Troubleshooting (Pemecahan Masalah)

### Port Terpakai?
```bash
lsof -i :8080  # Cari PID yang memakai port
kill -9 [PID]   # Hentikan proses tersebut
```

### Container Error?
```bash
podman logs -f payu-[service-name]  # Cek log spesifik
podman ps -a                        # Lihat status seluruh container
```

### Reset Total
```bash
podman-compose down
podman volume rm payu_postgres_data  # Hapus data DB (HATI-HATI!)
./scripts/setup-dev.sh --clean
```

Untuk panduan lebih lengkap, lihat **[docs/TROUBLESHOOTING.md](../TROUBLESHOOTING.md)**.

---

## 📖 9. Essential Resources

1. **[INDEX.md](../INDEX.md)** - Peta navigasi dokumentasi.
2. **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** - Memahami desain sistem.
3. **[CONTRIBUTING.md](./CONTRIBUTING.md)** - Git workflow & branch naming.
4. **[LESSONS.md](./LESSONS.md)** - Pola dan pelajaran teknis masa lalu.
5. **[AGENT_SKILLS_GUIDE.md](./AGENT_SKILLS_GUIDE.md)** - Cara bekerja dengan AI Assistant.

---

**Selamat berkarya di PayU! 🚀**
Jika ada kendala, jangan ragu bertanya di channel `#payu-dev`.

_Last Updated: February 24, 2026_
