# Reconciliation Patterns for Digital Banking

## 1. The Reconciliation Problem
Given two large datasets (Internal Logs vs External Bank Statement), identify:
- **Matched**: Exists in both with same status/amount.
- **Unmatched (Nostro Only)**: Exists in Bank Statement, missing in Internal (e.g., Deposit received but callback failed).
- **Unmatched (System Only)**: Exists Internal, missing in Bank (e.g., Disbursement sent but Bank failed/timeout).
- **Discrepancy**: Exists in both but amount/status differs.

## 2. Matching Algorithms

### A. One-to-One Matching (Perfect Match)
Primary Key: `ExternalReferenceID` (RRN / TrxId).
Secondary Key: `Amount`, `Date` (tolerance +/- 1 day).

```sql
SELECT
    s.trx_id, s.amount AS system_amt,
    b.amount AS bank_amt,
    CASE
        WHEN b.txn_ref IS NULL THEN 'MISSING_IN_BANK'
        WHEN s.amount != b.amount THEN 'AMOUNT_MISMATCH'
        ELSE 'MATCHED'
    END AS status
FROM system_ledger s
FULL OUTER JOIN bank_statement b ON s.external_ref = b.txn_ref;
```

### B. One-to-Many Matching (Batch Settlement)
Often Payment Gateways send **one** settlement transfer for **many** individual transactions.

**Logic**:
1. Sum all individual transactions with `settlement_batch_id = X`.
2. Compare sum against the single Settlement Row `X`.
3. If variance > 0: Mark entire batch as "Suspicious".

## 3. Automated Resolution (Self-Healing)

Don't just detect, FIX.

| Recon Result | Auto-Resolution Action | Ledger Entry |
| :--- | :--- | :--- |
| **Deposit Missing in System** | Credit User Wallet | Dr: Bank Nostro / Cr: User Wallet |
| **Disbursement Failed in Bank** | Refund User Wallet | Dr: Bank Nostro / Cr: User Wallet |
| **Fee Variance (< Rp 500)** | Write-off (Small Diff) | Dr: Expense / Cr: Bank Nostro |
| **Fee Variance (> Rp 500)** | Move to Suspense | Dr: Suspense / Cr: Bank Nostro |

## 4. Technical Implementation (Spring Batch)

For high-volume recon (>1M rows), use **Spring Batch** with Partitioning.

- **Reader**: Streaming read from CSV/DB (No `List<All>` in memory!).
- **Processor**: Apply matching logic.
- **Writer**: Update status in DB & Produce Kafka Event `ReconResultCreated`.
