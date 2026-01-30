# General Ledger & Accounting Standards

## 1. The Golden Rule of Banking: Double-Entry
For every transaction, the sum of debits must equal the sum of credits. 
**DEBITS = CREDITS**

### Accounting Equation in Banking
**Assets = Liabilities + Equity**

In a digital bank (PayU):
- **Assets**: Cash in Bank (Nostro), Loans Receivable.
- **Liabilities**: User Wallet Balances (the bank owes this to users).
- **Equity**: Principal capital and retained earnings.

## 2. Chart of Accounts (COA) Design
A hierarchical structure used to classify every financial transaction.

| Account Range | Category | PayU Example |
| :--- | :--- | :--- |
| **1xxxx** | **Assets** | 11001 (BCA Nostro), 12001 (Loans Outstanding) |
| **2xxxx** | **Liabilities** | 21001 (User Savings Pool), 22001 (Voucher Liabilities) |
| **3xxxx** | **Equity** | 31001 (Paid-up Capital), 32001 (Retained Earnings) |
| **4xxxx** | **Revenue** | 41001 (Admin Fees), 42001 (Loan Interest Income) |
| **5xxxx** | **Expense** | 51001 (SMS/OTP Costs), 52001 (Marketing Promo Costs) |

## 3. Standard Journal Entry Patterns

### Scenario A: User Topup via Virtual Account (BCA)
Users move money from their BCA account to PayU.
1. **Debit**: `11001 (BCA Nostro Asset)` - *PayU's cash in BCA increases.*
2. **Credit**: `21001 (User Savings Liability)` - *PayU's debt to the user increases.*

### Scenario B: Peer-to-Peer Transfer (Inside PayU)
User A sends Rp 100k to User B.
1. **Debit**: `21001 (User Savings Liability - User A)` - *Debt to User A decreases.*
2. **Credit**: `21001 (User Savings Liability - User B)` - *Debt to User B increases.*
*Note: This is a "Wash Trade" for the bank's total assets, only shifting liability.*

### Scenario C: Admin Fee Collection
PayU charges Rp 1.500 for a bill payment.
1. **Debit**: `21001 (User Savings Liability)` - *User's balance decreases.*
2. **Credit**: `41001 (Fee Revenue)` - *Bank's income increases.*

## 4. General Ledger (GL) Implementation (Event-Driven)

Microservices should **NOT** know the COA codes. They should emit **Business Events**.

**Adapter Pattern:**
1. `wallet-service` emits `WalletDebitedEvent(userId, amount, reason="PB_FEE")`.
2. `accounting-service` (GL Adapter) consumes the event.
3. GL Adapter looks up mapping: `PB_FEE` -> `Dr: 21001 / Cr: 41001`.
4. GL Adapter writes to `gl_entries` table.

## 5. Month-End Closing
Process to ensure all transactions for the month are captured and the trial balance is zero.
- **Accruals**: Recording expenses/revenue incurred but not yet paid (e.g., interest).
- **Amortization**: Spreading costs over time.
- **Revaluation**: Adjusting FX accounts (e.g., USD wallets) to current IDR rates.
