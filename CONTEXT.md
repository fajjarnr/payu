# PayU Domain Glossary

## Web Identity and BFF

- **BFF (Backend for Frontend)**: A server-side boundary that adapts authenticated backend capabilities for one frontend channel without exposing backend credentials to browser code.
- **Browser Session**: The authenticated relationship between a browser and PayU web application. Its client-visible identifier is not a user identity, role, or access token.
- **Access Token**: A short-lived credential used by a trusted server component to call an authorized backend service.
- **Refresh Token**: A longer-lived credential used only by the trusted authentication boundary to obtain a new access token or rotate a session.
- **Token Relay**: Server-side forwarding of a validated access token to an upstream service; browser code does not perform the relay.
- **CSRF Token**: A value submitted explicitly by browser code on state-changing requests to prove same-origin intent when authentication uses cookies.
- **OIDC Authorization Callback**: The one-time browser return from the identity provider after authorization-code and PKCE processing.
- **Route Handler**: A public HTTP endpoint in the web application that must perform its own authentication, authorization, input validation, and response policy.
- **Proxy**: An early request decision point used for routing or optimistic navigation checks. It is not the final authorization boundary.
- **Server Component**: A server-rendered UI component that can access server-only session context and should fetch protected data without an unnecessary browser round trip.

## Lending and Credit Scoring

- **CreditScoringFact**: Transient input fact for the Drools engine (kycStatus, tenureMonths, totalTransactions, totalAmount, successRate) with a mutable `score` accumulator via `addScore`/`subtractScore`; not persisted.
- **CreditScore**: Persisted snapshot (`credit_scores` table) — userId, score (BigDecimal, HALF_EVEN), riskCategory, lastCalculatedAt; single row per user (tenant-aware).
- **RiskCategory**: Derived enum from score tiers (EXCELLENT ≥750, GOOD ≥700, FAIR ≥650, POOR ≥600, VERY_POOR <600); produced by pricing DMN, not stored in Fact.
- **PreApproval**: Decision output for loan origination (requestedAmount vs maxApprovedAmount, minInterestRate, maxTenureMonths, estimatedMonthlyPayment, status, creditScore, riskCategory, validUntil +30d).
- **PreApprovalStatus**: Result of eligibility DMN — APPROVED / CONDITIONALLY_APPROVED / REJECTED.
- **LoanApplication**: User request (externalId, loanType, principalAmount, tenureMonths, purpose) that yields a Loan (status APPROVED / REJECTED / PENDING_APPROVAL); eligibility and pricing come from DMN, installment math stays in Java.
- **PricingTier**: Interest-rate band derived from creditScore (≥750→12%, ≥700→14%, ≥650→16%, else 18%) — owned by pricing DMN, duplicated previously in `LoanPreApprovalService` and `LendingApplicationService`.

## Wallet and Immutable Ledger

- **Wallet**: Materialized balance view per accountId (balance, reservedBalance, currency, version for optimistic locking); derived from ledger, not source of truth.
- **LedgerEntry**: Immutable append-only line in `ledger_entries` (id, transactionId, journalEntryId, accountId, coaCode, entryType DEBIT/CREDIT, amount BigDecimal 19,4 HALF_EVEN, balanceAfter, referenceType/Id, createdAt); no UPDATE/DELETE.
- **JournalEntry**: Atomic header for one business transaction (id, transactionId, createdAt) grouping 2+ LedgerEntries where sum DEBIT = sum CREDIT; enforced by `Journal.isBalanced()` and DB CHECK.
- **Chart of Accounts (CoA)**: Canonical codes (e.g., ASSET_WALLET, LIABILITY_CLEARING, SYSTEM_BI_FAST_CLEARING) — enum top-level file, not inner class.
- **Available Balance**: `balance - reservedBalance`; invariant `available >= 0`; `reservedBalance` tracks holds (reserve/commit/release) without mutating ledger.
- **Reversal Entry**: Correction via new JournalEntry with opposite entries (referenceType=REVERSAL, referenceId=originalJournalId); never DELETE ledger.

## Transaction Orchestration (transaction-service)

- **Transaction (Orchestration Record)**: Mutable state machine in `transactions` table (`backend/transaction-service/src/main/java/id/payu/transaction/adapter/persistence/entity/TransactionEntity.java:34`) — `PENDING → VALIDATING → PENDING → COMPLETED/FAILED/CANCELLED` with `version` optimistic locking, `referenceNumber` unique, `idempotencyKey` dedup, `reservationId` for wallet hold. **Bukan sumber kebenaran saldo** — saldo truth ada di `wallet-service` Ledger. Mutasi status wajib via `outbox-starter` CloudEvents `payu.transaction.*.v1` (lihat ADR-0041, ADR-0049).
- **Transfer**: Pergerakan dana **internal PayU** antar wallet — atomik 1-hop `walletServicePort.transferBalance()` (debit+credit satu TX, idempoten by reference), tanpa reserve/saga. Lihat `InitiateTransferCommandHandler.java: processInternalTransfer`.
- **Disbursement (Payout)**: Pencairan ke bank eksternal via BI-FAST/SKN/RTGS — async, 2-phase: `reserveBalance` → call rail → `settleInterbankTransfer` (`SELECT FOR UPDATE` on `referenceNumber`) → `commit/release`. Punya `disbursements` + `batch_disbursements` tabel sendiri, idempoten via `UNIQUE(idempotency_key)`. Beda bounded context dengan `BillingService::BillPayment` (PLN/PDAM) dan `LoanOrigination::Disbursement` (pencairan kredit) — jangan campur.
- **Payment (Collection)**: Uang masuk via QRIS/VA — `QRIS_PAYMENT`, `VirtualAccount` (`virtual_accounts` table). Status `PENDING → PAID/EXPIRED`. Callback wajib HMAC + `X-Idempotency-Key` + `FOR UPDATE`.
- **BillPayment (billing-service)**: Pembayaran tagihan ke biller eksternal (PLN, PDAM, BPJS, pulsa) — domain `backend/billing-service/src/main/java/id/payu/billing/domain/model/BillPayment.java:13` dengan `BillerType` enum. Rail via `biller-simulator`, bukan via `transaction-service`.
- **SplitBill**: Social payment — `split_bills` + `split_bill_participants` (account nullable, version optimistic locking). Bukan core ledger, hitung sharing di aplikasi, settlement tetap via Transfer/Disbursement.
- **IdempotencyKey**: Client-supplied `X-Idempotency-Key` (header `Idempotency-Key` per SNAP-BI) yang di-enforce di DB `UNIQUE(tenant_id, idempotency_key)` + interceptor `IdempotencyInterceptor` + `findByIdempotencyKey` lookup. Replay return existing result tanpa double posting — lihat ADR-0022, ADR-0060.
- **ReferenceNumber vs ReservationId**: `referenceNumber` = external rail reference (UNIQUE, untuk `settleInterbankTransfer` & inquiry `GET /snap/v1.0/transfer/status`), `reservationId` = hold di wallet (`reserveBalance`), `idempotencyKey` = dedup boundary client.
- **Reconciliation**: Job periodek membandingkan `transactions` (core) vs BI-FAST member statement vs bank settlement account (three-way, lihat ADR-0060). PADG 14/2025 wajib ≥1×/hari; break auto-flag, koreksi via reversal JournalEntry, bukan UPDATE.
