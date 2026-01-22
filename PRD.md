# Product Requirements Document (PRD)
# PayU Digital Banking Platform

## 1. Executive Summary

### 1.1 Product Vision
PayU adalah platform digital banking modern yang memberikan pengalaman perbankan yang mudah, cepat, dan aman untuk generasi digital. Terinspirasi dari kesuksesan Bank Jago dan blu by BCA, PayU hadir dengan fitur-fitur inovatif dan antarmuka yang intuitif.

### 1.2 Product Name
- **Bahasa Indonesia**: PayU (dibaca: "payu" - bahasa Jawa yang berarti "laku/berhasil")
- **English**: PayU (pay + you, emphasizing personalized banking)

### 1.3 Target Market
- Millennials dan Gen Z (18-40 tahun)
- Urban professionals dan digital natives
- Small business owners dan freelancers
- Pengguna yang mencari solusi banking digital-first

## 2. Product Goals & Objectives

### 2.1 Primary Goals
1. Menyediakan platform digital banking yang 100% mobile-first
2. Memberikan pengalaman user yang seamless dan intuitif
3. Menawarkan fitur financial management yang komprehensif
4. Membangun ekosistem financial services yang terintegrasi

### 2.2 Success Metrics (KPIs)
- User acquisition: 100K users dalam 6 bulan pertama
- Daily Active Users (DAU): 60% dari total users
- Transaction volume: Rp 1 Triliun dalam tahun pertama
- Customer satisfaction score: > 4.5/5.0
- App store rating: > 4.7/5.0

## 3. Core Features

### 3.1 Account Management
#### 3.1.1 Digital Account Opening
- **eKYC (Electronic Know Your Customer)**
  - Verifikasi identitas dengan NIK dan foto selfie
  - OCR untuk scan KTP otomatis
  - Liveness detection untuk mencegah fraud
  - Verifikasi data dengan Dukcapil
  - Onboarding dalam < 5 menit

- **Multi-Pocket/Kantong System**
  - Main Account (tabungan utama)
  - Saving Pockets (hingga 10 kantong)
  - Custom pocket names dan goals
  - Auto-save rules dan scheduled transfers
  - Shared pockets untuk joint savings

#### 3.1.2 Account Types
- Personal Account (Perorangan)
- Business Account (untuk UMKM)
- Joint Account (Rekening bersama)

### 3.2 Payment & Transfer Features

#### 3.2.1 Instant Transfer
- Transfer antar PayU accounts (real-time, gratis)
- BI-FAST transfer ke bank lain (real-time, 24/7)
- SKN/RTGS transfer
- Scheduled transfers
- Recurring payments

#### 3.2.2 QR Payment
- QRIS payment dan receive
- Dynamic QR untuk merchant
- Split bill feature
- P2P payment via QR

#### 3.2.3 Virtual Cards
- Virtual debit card untuk online shopping
- Multiple virtual cards untuk budgeting
- Instant freeze/unfreeze
- Transaction limits per card

### 3.3 Bill Payment & Top-Up
- Pulsa dan paket data
- Listrik PLN
- PDAM
- Internet & TV cable
- Cicilan (kredit, pinjaman)
- Asuransi
- E-wallet top-up (GoPay, OVO, Dana, dll)

### 3.4 Financial Management

#### 3.4.1 Budget Tracker
- Automatic expense categorization
- Monthly budget setting
- Spending alerts
- Visual analytics dan reports
- Custom categories

#### 3.4.2 Goals & Savings
- Multiple savings goals
- Target amount dan deadline
- Progress tracking
- Auto-save recommendations
- Goal achievement rewards

#### 3.4.3 Financial Insights
- Cash flow analysis
- Spending patterns
- Income vs expense reports
- Bill predictions
- Personalized financial tips

### 3.5 Investment Features
- Deposito digital dengan tenor fleksibel
- Reksadana marketplace
- Emas digital
- SBN (Surat Berharga Negara)
- Robo-advisory untuk pemula

### 3.6 Loan & Credit
- Personal loan (Pinjaman tunai)
- PayLater untuk transaksi
- Cicilan untuk pembelian besar
- Credit score tracking
- Pre-approved loan offers

### 3.7 Rewards & Cashback
- Poin rewards untuk setiap transaksi
- Cashback untuk merchant tertentu
- Referral program
- Daily check-in rewards
- Gamification elements

## 4. Technical Requirements

### 4.1 Platform Support
- iOS app (iOS 14+)
- Android app (Android 8.0+)
- Web application (responsive)
- API untuk third-party integration

### 4.2 Performance Requirements
- App launch time: < 2 detik
- Transaction processing: < 3 detik
- API response time: < 500ms (p95)
- Uptime: 99.95%
- Support concurrent users: 100K+

### 4.3 Security Requirements
- End-to-end encryption untuk semua transaksi
- Multi-factor authentication (MFA)
- Biometric authentication (fingerprint, face ID)
- Device binding
- Transaction PIN
- Session timeout (5 menit inaktif)
- Anti-fraud detection system
- SOC 2 Type II compliance
- PCI DSS compliance
- ISO 27001 certified

### 4.4 Compliance & Regulatory
- OJK (Otoritas Jasa Keuangan) licensed
- BI (Bank Indonesia) regulations compliant
- POJK compliance
- AML/CFT (Anti-Money Laundering) procedures
- GDPR untuk data protection

## 5. User Experience (UX) Requirements

### 5.1 Design Principles
- **Simple & Clean**: Minimalist design dengan fokus pada usability
- **Fast**: Minimal steps untuk complete transactions
- **Personalized**: Adaptive interface berdasarkan user behavior
- **Accessible**: WCAG 2.1 AA compliant

### 5.2 Key User Flows
1. **Onboarding**: Welcome → eKYC → Account Created → Tutorial
2. **Transfer**: Home → Transfer → Select Contact → Amount → Confirm → Success
3. **Bill Payment**: Home → Bills → Select Biller → Amount → Confirm → Success
4. **Savings Goal**: Pockets → Create Pocket → Set Goal → Auto-save Setup → Done

### 5.3 UI Components
- Bottom navigation (5 tabs maximum)
- Card-based layouts
- Contextual actions (swipe gestures)
- Real-time notifications
- In-app chat support

## 6. Customer Support

### 6.1 Support Channels
- 24/7 in-app chat support
- Phone hotline
- Email support
- Social media (Twitter, Instagram)
- Help center dengan FAQ
- Video tutorials

### 6.2 Self-Service
- Comprehensive FAQ
- Transaction dispute form
- Card block/unblock
- Password reset
- Statement download

## 7. Marketing Features

### 7.1 Acquisition
- Referral code system
- Social sharing untuk achievements
- App store optimization (ASO)
- Deep linking untuk campaigns

### 7.2 Retention
- Push notifications (smart, tidak spammy)
- Email campaigns
- In-app messages
- Personalized offers

## 8. Analytics & Monitoring

### 8.1 User Analytics
- User journey tracking
- Feature usage statistics
- Conversion funnel analysis
- Cohort analysis
- Churn prediction

### 8.2 Business Analytics
- Transaction volume & value
- Revenue metrics
- Customer acquisition cost (CAC)
- Lifetime value (LTV)
- Product performance dashboard

## 9. Phased Rollout Plan

### Phase 1 (MVP - Month 1-6)
- Basic account opening & eKYC
- Transfer (antar PayU & BI-FAST)
- Basic bill payment
- Single pocket system
- Virtual debit card
- **TokoBapak Integration (External Payment Provider)**

### Phase 2 (Month 7-12)
- Multi-pocket system
- QR payment (QRIS)
- Budget tracker
- Deposito digital
- Cashback & rewards program

### Phase 3 (Month 13-18)
- Investment features (reksadana, emas)
- Personal loan
- Business account
- Advanced analytics
- Robo-advisory

### Phase 4 (Month 19-24)
- PayLater & installment
- Joint account
- API marketplace untuk third-party
- International remittance
- Premium tier membership

## 10. Dependencies & Integrations

### 10.1 External Services
- Dukcapil (verifikasi KTP)
- BI-FAST (real-time transfer)
- QRIS (QR payment network)
- Payment gateway (card transactions)
- SMS gateway (OTP)
- Email service provider

### 10.2 Third-Party APIs
- Credit scoring agencies
- Investment platforms
- Insurance providers
- Merchant partnerships

## 11. Risks & Mitigation

### 11.1 Technical Risks
- **Risk**: System downtime
  - **Mitigation**: Multi-region deployment, auto-failover, 24/7 monitoring

- **Risk**: Security breach
  - **Mitigation**: Regular security audits, penetration testing, bug bounty program

### 11.2 Business Risks
- **Risk**: Low user adoption
  - **Mitigation**: Aggressive marketing, referral incentives, superior UX

- **Risk**: Regulatory changes
  - **Mitigation**: Dedicated compliance team, regulatory monitoring

## 12. Success Criteria

### 12.1 Launch Criteria
- [x] OJK license approved
- [x] Security audit passed
- [x] Load testing completed (100K concurrent users)
- [x] Beta testing with 1000 users completed
- [x] All core features functional
- [x] Customer support team trained
- [x] Local Infrastructure: Docker Compose verification (up/down) completed
- [x] Disaster Recovery Plan (DRP) & Backup-Restore verified
- [x] Production Monitoring & Alerting (LokiStack/Prometheus) operational
- [x] Final Penetration Testing (Pentest) Report signed off
- [x] PCI-DSS & OJK Regulatory Compliance Audit completed
- [ ] **Production Deployment**: Successful deployment & verification on Red Hat OpenShift
- [ ] **Mobile App Stores**: iOS App Store & Google Play Store approval obtained
- [ ] **Legal Readiness**: Terms of Service & Privacy Policy published
- [ ] **Security Hardening**: Secrets management (Vault) & CI/CD pipelines verified

### 12.2 Post-Launch Success
- 100K users within 6 months
- < 5% monthly churn rate
- 90% of transactions successful
- < 1% fraud rate
- Customer satisfaction > 4.5/5.0

## 13. Appendix

### 13.1 Glossary
- **eKYC**: Electronic Know Your Customer
- **BI-FAST**: Bank Indonesia Fast Payment
- **QRIS**: Quick Response Code Indonesian Standard
- **SKN**: Sistem Kliring Nasional
- **RTGS**: Real-Time Gross Settlement

### 13.2 References
- Bank Indonesia regulations
- OJK digital banking guidelines
- Competitor analysis (Jago, blu, Jenius)
- User research findings

---

## 14. Technical Architecture Overview

> Detail lengkap tersedia di [ARCHITECTURE.md](./ARCHITECTURE.md)

### 14.1 Microservices Stack

| Service | Technology | Domain |
|---------|------------|--------|
| `account-service` | Java Spring Boot 3.4 | User accounts, eKYC, multi-pocket (Hexagonal) |
| `auth-service` | Java Spring Boot 3.4 + Red Hat SSO | Authentication, MFA, OAuth2 |
| `transaction-service` | Java Spring Boot 3.4 | Transfers, BI-FAST, QRIS (Hexagonal) |
| `wallet-service` | Java Spring Boot 3.4 | Balance management, ledger (Hexagonal) |
| `billing-service` | Java Quarkus 3.x Native | Bill payments, top-ups |
| `notification-service` | Java Quarkus 3.x Native | Push, SMS, Email notifications |
| `gateway-service` | Java Quarkus 3.x Native | API Gateway, Rate Limiting, Circuit Breaker |
| `kyc-service` | Python FastAPI 3.12 | OCR, liveness detection ML (UBI-based) |
| `analytics-service` | Python FastAPI 3.12 | User insights, ML recommendations (UBI-based) |

#### 14.1.2 Upcoming Services (Roadmap)

| Service | Category | Domain | Priority |
|---------|----------|--------|----------|
| `investment-service` | Financial | Digital Deposits, Mutual Funds, Gold | Phase 2/3 |
| `lending-service` | Financial | Personal Loan, PayLater, Credit Underwriting | Phase 3/4 |
| `promotion-service` | Engagement | Rewards, Cashback, Referral, Loyalty Points | Phase 2 |
| `backoffice-service` | Operations | Manual KYC Review, Fraud Monitoring, Customer Ops | Operational |
| `partner-service` | Ecosystem | B2B Standard API (SNAP BI), Merchant Portal | Ecosystem |

### 14.2 Infrastructure

| Component | Technology |
|-----------|------------|
| Container Orchestration | Red Hat OpenShift 4.20+ (on-prem/multi-cloud) |
| Message Broker | AMQ Streams (Apache Kafka) |
| Message Queue | AMQ Broker (ActiveMQ Artemis) |
| Database | Crunchy PostgreSQL 16 (Standard SQL + JSONB) |
| Caching | Red Hat Data Grid (RESP mode / Redis API) |
| Service Mesh | OpenShift Service Mesh (Istio/mTLS) |
| API Gateway | Quarkus Native Gateway |
| Identity Provider | Red Hat SSO (Keycloak) |
| Observability | OpenShift Logging (LokiStack) & Monitoring (Prometheus/Grafana) |

---

## 15. TokoBapak Integration

PayU akan terintegrasi dengan [TokoBapak](../tokobapak) e-commerce platform sebagai **External Banking Provider**.

### 15.1 Integration Pattern

```
TokoBapak payment-service ───► PayU API Gateway ───► PayU Transaction Service
                          ◄─── Webhook Callback ◄───
```

### 15.2 Payment Flow

1. **Customer checkout** di TokoBapak memilih "Bayar dengan PayU"
2. **payment-service** creates payment request to PayU API
3. **PayU** returns payment URL untuk redirect
4. **Customer** completes payment di PayU app/web
5. **PayU** sends webhook callback ke TokoBapak
6. **payment-service** updates order status

### 15.3 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/partner/auth/token` | POST | Get access token |
| `/v1/partner/payments` | POST | Create payment |
| `/v1/partner/payments/{id}` | GET | Get payment status |
| `/v1/partner/payments/{id}/refund` | POST | Refund payment |

### 15.4 Webhook Events

| Event | Description |
|-------|-------------|
| `payment.completed` | Payment successful |
| `payment.failed` | Payment failed |
| `payment.expired` | Payment expired |
| `refund.completed` | Refund processed |

---

**Document Version**: 1.1  
**Last Updated**: January 2026  
**Owner**: Product Team PayU  
**Status**: Approved