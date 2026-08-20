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
