# PayU Digital Banking - Panduan Penggunaan

> **Platform Digital Banking Microservices**
> Terakhir diperbarui: 5 Februari 2026

---

## 🚀 Quick Start

### 1. Menjalankan Semua Services

```bash
# Dari root directory project
cd /home/ubuntu/payu

# Start semua services (backend, frontend, infrastructure)
podman compose up -d

# Tunggu semua services healthy (±2-3 menit)
podman ps
```

### 2. Mengakses Aplikasi

| Service | URL | Keterangan |
|:--------|:-----|:-----------|
| **Web App** | http://localhost:3001 | Aplikasi frontend utama |
| **API Gateway** | http://localhost:8080 | Backend API gateway |
| **Keycloak Admin** | http://localhost:8099 | Manajemen user & realm |

---

## 🔐 Kredensial Login

### **Keycloak Admin Console**

Untuk manajemen user, realm, dan client:

| Field | Value |
|:-------|:-------|
| URL | http://localhost:8099 |
| **Username** | `admin` |
| **Password** | `P@ssw0rd123` ✅ (Updated Feb 6, 2026) |
| Realm | `master` |

> ⚠️ **PRODUCTION**: Ganti password Keycloak admin untuk production! Password ini tersimpan di database Keycloak.

---

### **Customer Login (Aplikasi Web)**

Gunakan akun berikut untuk login ke aplikasi PayU:

#### **Customer 1 (User Demo)**

| Field | Value |
|:-------|:-------|
| **Username/Phone** | `customer1` |
| **Password** | `P@ssw0rd123` ✅ (Updated Feb 6, 2026) |
| **Email** | `customer1@payu.fajjjar.my.id` |

> **Note**: Password standar untuk semua dummy user adalah `P@ssw0rd123`.

#### **Admin Backend (Opsional)**

| Field | Value |
|:-------|:-------|
| **Username** | `admin` |
| **Password** | `P@ssw0rd123` ✅ |
| **Role** | Administrator |

---

## 📱 Alur Penggunaan Aplikasi

### **Pertama Kali: Register Akun Baru**

1. Buka http://localhost:3001
2. Klik **"Daftar"** atau **"Register"**
3. Isi form pendaftaran:
   - Nama Lengkap
   - Email
   - Nomor HP (gunakan format: `+628...`)
   - Password
4. Klik **"Daftar"**
5. Akun akan dibuat di Keycloak

### **Login ke Aplikasi**

1. Buka http://localhost:3001
2. Masukkan kredensial:
   - Username: `customer1`
   - Password: `P@ssw0rd123` ✅
3. Klik **"Login"**

### **Setelah Login: Fitur Utama**

| Fitur | Deskripsi | Endpoint |
|:------|:----------|:---------|
| **Dashboard** | Ringkasan saldo & aktivitas | `/dashboard` |
| **Transfer** | Transfer antar rekening | `/transfer` |
| **QRIS** | Pembayaran QR Code | `/qris` |
| **Investment** | Investasi & reksadana | `/investments` |
| **Lending** | Pinjaman & PayLater | `/lending` |
| **Bills** | Bayar tagihan (PLN, PDAM) | `/bills` |
| **Cards** | Manajemen kartu debit | `/cards` |
| **Settings** | Pengaturan profil & e-statement | `/settings` |

---

## 🔧 Management & Monitoring

### **Check Service Health**

```bash
# Cek semua container status
podman ps

# Cek health endpoint
curl http://localhost:8080/actuator/health
```

### **View Logs**

```bash
# Logs dari service tertentu
podman logs payu-gateway-service -f
podman logs payu-account-service -f
podman logs payu-web-app -f

# Logs dari semua services
podman compose logs -f
```

### **Stop Services**

```bash
# Stop semua services
podman compose down

# Stop specific service
podman stop payu-web-app
```

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        PAYU PLATFORM                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐                │
│  │  Web App │──▶│ Gateway  │──▶│ Services │                │
│  │ (Next.js)│   │ (Quarkus)│   │(Spring)  │                │
│  └──────────┘   └────┬─────┘   └────┬─────┘                │
│                      │               │                       │
│                      ▼               ▼                       │
│              ┌──────────┐   ┌──────────────┐                │
│              │ Keycloak │   │  PostgreSQL  │                │
│              │   (IAM)  │   │  (Database)  │                │
│              └──────────┘   └──────────────┘                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧪 Testing

### **Run E2E Tests**

```bash
# Masuk ke frontend directory
cd frontend/web-app

# Install Playwright browsers
npx playwright install --with-deps

# Jalankan E2E tests
npx playwright test

# Jalankan dengan UI mode
npx playwright test --ui
```

### **Run Backend Tests**

```bash
# Test semua backend services
cd /home/ubuntu/payu
mvn -f backend/pom.xml test

# Test spesifik service
mvn -f backend/account-service/pom.xml test
```

---

## 📚 API Documentation

### **OpenAPI/Swagger UI**

| Service | URL |
|:--------|:-----|
| API Portal | http://localhost:8080/api-docs |
| Account Service | http://localhost:8081/swagger-ui.html |
| Transaction Service | http://localhost:8003/swagger-ui.html |

### **Keycloak API**

```bash
# Get token untuk customer1 via Keycloak
curl -X POST "http://localhost:8099/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=customer1&password=P@ssw0rd123&grant_type=password&client_id=payu-backend&client_secret=payu-backend-secret-2026"

# Atau via auth-service (recommended)
curl -X POST "http://localhost:8002/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"customer1","password":"P@ssw0rd123"}'
```

---

## 🐛 Troubleshooting

### **Masalah: Login Gagal**

1. Cek Keycloak status:
   ```bash
   podman ps | grep keycloak
   ```

2. Cek realm `payu` exists:
   ```bash
   curl http://localhost:8099/realms/payu
   ```

3. Reset password user via Keycloak Admin:
   - Login ke http://localhost:8099
   - Realm: `payu`
   - Users → View user → Credentials → Set password

### **Masalah: Service Unhealthy**

```bash
# Cek logs service
podman logs payu-[service-name] -f

# Restart service
podman restart payu-[service-name]
```

### **Masalah: Database Connection**

```bash
# Cek PostgreSQL status
podman ps | grep postgres

# Cek database connection
podman exec payu-postgres pg_isready -U payu
```

---

## 🔒 Security Notes

### **For Production Deployment:**

1. ✅ Ganti semua default passwords
2. ✅ Gunakan HTTPS untuk semua endpoints
3. ✅ Konfigurasi firewall & network policies
4. ✅ Enable rate limiting
5. ✅ Setup proper secret management (Vault/Sealed Secrets)
6. ✅ Enable audit logging
7. ✅ Configure backup & disaster recovery

### **Passwords to Change:**

| Component | Environment Variable | Default Value |
|:----------|:---------------------|:--------------|
| Keycloak Admin | `KEYCLOAK_ADMIN_PASSWORD` | `your_secure_keycloak_admin_password_here_minimum_32_chars` |
| PostgreSQL | `POSTGRES_PASSWORD` | `payu_secret` |
| Redis | `REDIS_PASSWORD` | (empty) |

---

## 📞 Support

Untuk pertanyaan atau issue:
- GitHub Issues: https://github.com/fajjarnr/payu/issues
- Documentation: `/home/ubuntu/payu/docs/`

---

*PayU Digital Banking Platform - Development Environment*
