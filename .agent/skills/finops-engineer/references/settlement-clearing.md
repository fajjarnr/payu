# Settlement and Clearing Operations

## 1. Concepts
- **Clearing**: The process of transmitting, reconciling, and, in some cases, confirming payment orders prior to settlement.
- **Settlement**: The actual transfer of funds between the payer and the payee (typically between their banks).

## 2. Netting Strategy
Instead of sending 1.000 separate transfers to a partner (e.g., PLN for bill payments), PayU calculates the **Net Position**.

**Example:**
- Users pay PLN bills via PayU: Total Rp 500M (PayU owes PLN).
- PLN pays PayU commission: Total Rp 2M (PLN owes PayU).
- **Net Settlement**: PayU transfers Rp 498M to PLN.

## 3. Funds Flow Patterns

### A. T+0 (Real-time Settlement)
Used for BI-FAST and QRIS (for some aggregators). Funds must be moved immediately.

### B. T+n (Deferred Settlement)
Standard for Credit Cards and Batch Billers.
- **T+1**: Funds arrive next business day.
- **T+3**: Standard for some international networks.

## 4. Partner Liquidity Management
FinOps must monitor "Partner Balances" (Float) to ensure service continuity.

- **Low Balance Alert**: If PayU's float at a vendor (e.g., Telkomsel for pulses) drops below Rp 10M, trigger an automated Top-up request to Treasury.
- **Curbing Risk**: If a partner's payable to PayU grows too large without settlement, the system should automatically throttle or disable that partner.

## 5. Settlement Batch Processing
1. **Trigger**: Time-based or Volume-based.
2. **Aggregation**: Sum up all `PENDING_SETTLEMENT` transactions for the partner.
3. **Validation**: Check against internal Recon result.
4. **Execution**: Generate Settlement Instruction (PAIN.001 ISO20022 or Bank API call).
5. **Confirmation**: Receive Settlement Advice (CAMT.054) and mark transactions as `SETTLED`.
