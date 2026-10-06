# Product

<!-- impeccable:product-schema 1 -->

## Platform

adaptive

## Stack

Existing codebase: Java 21 (Spring Boot 3.4 core banking, Quarkus supporting services), Python 3.12 (FastAPI for KYC/analytics), TypeScript (Next.js web app, Expo/React Native mobile), PostgreSQL 16, Kafka, Redis, Keycloak, Red Hat OpenShift 4.22+.

## Users

Primary: Millennials and Gen Z (18–40) in Indonesia — digital natives, mobile-first, want simplicity and speed. Secondary: urban professionals, small business owners (UMKM), freelancers.

## Product Purpose

PayU is a standalone digital banking platform for Indonesia. It provides account opening (eKYC), multi-pocket savings, instant transfers (BI-FAST, QRIS, internal), bill payments, investments (deposito, reksadana, emas), lending (PayLater, personal loans), and financial management tools. Success means: 100K users in 6 months, 60% DAU, Rp 1 trillion transaction volume in year one, >4.5/5.0 satisfaction.

## Positioning

Full-stack digital banking with enterprise-grade infrastructure (microservices, event-driven, hexagonal architecture) delivered as a consumer-grade mobile-first experience. Inspired by Bank Jago and blu by BCA. Name "PayU" from Javanese "laku/berhasil" (successful).

## Operating Context

- Mobile-first: bottom navigation (5 tabs max), card-based layouts, swipe gestures, biometric auth
- Web app: responsive Next.js for account management and transactions
- Regulatory: OJK licensed, BI compliant, POJK, AML/CFT, UU PDP (GDPR-equivalent)
- Security: MFA, device binding, transaction PIN, 5-min session timeout, anti-fraud ML
- Deployment: Red Hat OpenShift 4.22+, 20+ microservices, Kafka event bus, Istio mTLS

## Capabilities and Constraints

- eKYC: OCR KTP, liveness detection, Dukcapil verification, <5 min onboarding
- Multi-pocket: up to 10 savings pockets, custom names, auto-save rules, shared pockets
- Transfers: internal (real-time, free), BI-FAST, SKN/RTGS, scheduled, recurring
- QRIS: payment, receive, dynamic QR for merchants, split bill
- Virtual debit cards: multiple cards, instant freeze/unfreeze, per-card limits
- Bill payment: PLN, PDAM, pulsa, internet, cicilan, asuransi, e-wallet top-up
- Investments: deposito, reksadana, emas, SBN, robo-advisory
- Lending: personal loan, PayLater, installments, credit score tracking
- Budget tracker: auto-categorization, monthly budgets, spending alerts, analytics
- Rewards: points, cashback, referral program, gamification
- Constraints: BigDecimal for money (never float/double), immutable ledger (double-entry), idempotency keys on all payment endpoints, RFC 9457 error format, WCAG 2.1 AA

## Brand Commitments

- Name: PayU (Javanese: "laku/berhasil")
- Language: Indonesian (Bahasa Indonesia) primary, English secondary
- Voice: simple, fast, personalized, accessible
- No existing brand assets or visual identity constraints beyond the PRD

## Evidence on Hand

- PRD: `docs/product/PRD.md`
- Feature catalog: `docs/product/FEATURES.md`
- Architecture: `docs/architecture/ARCHITECTURE.md`
- 21 ADRs: `docs/adr/`
- API docs: `docs/api/`
- No user research data, no analytics, no customer testimonials, no brand guidelines document

## Product Principles

1. **Mobile-first, always**: Design for the smallest screen first; web is an extension
2. **Speed as a feature**: <2s app launch, <3s transaction processing, minimal steps
3. **Trust through security**: Biometric auth, transparent security, no dark patterns
4. **Financial clarity**: Users always know their balance, spending, and goals
5. **Indonesian identity**: Local language, local payment methods (QRIS, BI-FAST), local context

## Accessibility & Inclusion

WCAG 2.1 AA compliance required. Indonesian language primary. Biometric auth (fingerprint, face ID) as primary authentication method.
