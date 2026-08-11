# ADR-0018: KYC — Hybrid Model (PayU as KYC Service)

**Status**: Accepted  
**Date**: 2026-05-07  
**Deciders**: PayU Engineering Team

## Context

PayU serves multiple project clients (TokoBapak, Nobar) that need KYC/eKYC verification. Question: should PayU handle all KYC data centrally, or delegate to project clients?

## Decision

**Hybrid Model — PayU provides KYC as a Service with webhook notifications.**

### PayU Responsibilities
- NIK verification via Dukcapil simulator (→ Dukcapil API in production)
- Liveness detection (selfie + face matching)
- Basic KYC status: `NOT_STARTED → PENDING → VERIFIED / REJECTED`
- Webhook notification to project clients when KYC status changes

### Client Responsibilities
- Store KYC results if needed for their own compliance
- Extend verification with business-specific steps (business license, NPWP, etc.)
- Manage customer onboarding flow in their own UI

### API Design (SNAP-BI inspired)
```
POST   /v1/partner/kyc/verify          # Initiate KYC
GET    /v1/partner/kyc/{id}            # Check status
POST   /v1/partner/webhooks/kyc        # Register webhook for KYC events
```

### Event Flow
```
Client initiates KYC → kyc-service verifies → status updated
    → Kafka event: payu.kyc.verified / payu.kyc.rejected
    → Webhook callback to client: POST {client_url}/kyc/callback
```

## Consequences

- ✅ Single source of truth for core KYC (PayU)
- ✅ Clients retain flexibility for business-specific verification
- ✅ Clear API contract between PayU and clients
- ⚠️ PayU must implement webhook retry + idempotency
- ⚠️ Clients must handle webhook delivery
