# PayU Digital Banking Platform

> Platform digital banking modern untuk generasi digital Indonesia

[![License](https://img.shields.io/badge/license-Proprietary-red.svg)]()
[![Status](https://img.shields.io/badge/status-In%20Development-yellow.svg)]()

---

## 📋 Overview

**PayU** (bahasa Jawa: "laku/berhasil") adalah platform digital banking yang menyediakan pengalaman perbankan yang mudah, cepat, dan aman. Terinspirasi dari Bank Jago dan blu by BCA, PayU hadir dengan arsitektur microservices yang production-ready.

## 🎯 Key Features

- **Digital Account Opening** - eKYC dalam < 5 menit
- **Multi-Pocket System** - Kelola hingga 10 kantong tabungan
- **Instant Transfer** - BI-FAST, QRIS, dan transfer internal
- **Bill Payment** - PLN, PDAM, Pulsa, dan lainnya
- **Financial Management** - Budget tracker, goals, dan insights
- **Virtual Cards** - Kartu virtual untuk belanja online

## 🏗️ Architecture

PayU dibangun dengan arsitektur microservices modern:

| Service | Technology | Domain |
|---------|------------|--------|
| `account-service` | Java Spring Boot | User accounts, eKYC |
| `auth-service` | Java Spring Boot + Keycloak | Authentication, MFA |
| `transaction-service` | Java Spring Boot | Transfers, payments |
| `wallet-service` | Java Spring Boot | Balance, ledger |
| `notification-service` | NestJS | Push, SMS, Email |
| `kyc-service` | Python FastAPI | OCR, ML |

Lihat [ARCHITECTURE.md](./ARCHITECTURE.md) untuk detail lengkap.

## 📁 Project Structure

```
payu/
├── ARCHITECTURE.md     # Technical architecture documentation
├── CHANGELOG.md        # Version history
├── PRD.md              # Product Requirements Document
├── README.md           # This file
└── (services TBD)      # Microservices implementation
```

## 🔗 Integration

PayU terintegrasi dengan **TokoBapak** e-commerce platform sebagai External Banking Provider:

```
TokoBapak payment-service ───► PayU API ───► PayU Transaction Service
                          ◄─── Webhook ◄───
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [PRD.md](./PRD.md) | Product Requirements Document |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Technical Architecture |
| [CHANGELOG.md](./CHANGELOG.md) | Version History |

## 🛡️ Compliance

- PCI DSS Level 1
- ISO 27001
- OJK Digital Banking License (target)
- BI-FAST Participation (target)

---

**© 2026 PayU Digital Banking**
