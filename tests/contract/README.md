# Contract Testing with Spring Cloud Contract

Reference copies of the Spring Cloud Contract verifier contracts. The
authoritative sources live in each provider's
`src/test/resources/contracts/` (service poms bind `spring-cloud-contract-maven-plugin`
with a `contract-test` Maven profile that runs the generated `ContractVerifierTest`).

## Contracts (2026-08-13)

| Service | Happy path | Error case (RFC 9457 `application/problem+json`) |
| :--- | :--- | :--- |
| transaction-service | `createTransfer` (201) | `createTransferInvalidAmount` (400 — validation) |
| wallet-service | `getBalance` (200) | `getBalanceNotFound` (404) |
| auth-service | `loginUser` (200) | `loginUserMissingCode` (400 — validation) |
| billing-service | `getPayment` (200) | `getPaymentNotFound` (404) |
| partner-service (SNAP-BI) | — (butuh HMAC live) | `snapBiAuthTokenExpiredTimestamp` / `snapBiPaymentExpiredTimestamp` / `snapBiRefundExpiredTimestamp` (4002508) |

401 cases are exercised by the service security suites (QAMVP-014), not the
verifier: `ContractVerifierBase` classes set a JWT principal and disable the
security filter chain, so unauthenticated flows are out of scope for the
provider-side verifier.

SNAP-BI happy paths need a live HMAC signature + a within-5-minutes
`X-TIMESTAMP`, which cannot be frozen in a contract; the deterministic
contracts cover the SNAP request/error wire shape (expired timestamp →
`4002508`). CloudEvents wire contract lives in outbox-starter
(`CloudEventsContractTest`, ce-specversion 1.0.2).

## Running

```bash
# Transaction (also runs -am reactor deps)
mvn -B -f backend/pom.xml -pl transaction-service -am test \
  -Pcontract-test -Dtest=ContractVerifierTest -Dsurefire.failIfNoSpecifiedTests=false

# Wallet / auth: same command with the respective service id
```

CI: `.github/workflows/contract-tests.yml` runs the verifier for the three
services on push/PR touching contracts, poms, or the workflow. Requires
Rest Assured ≥ 5.5.7 (Spring 7 compatibility — see parent `pom.xml`).
