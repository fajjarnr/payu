# ISO-8583 Mapping Standards for PayU

## 1. Overview
ISO-8583 is the international standard for financial transaction card originated messages. Within PayU, we map these legacy formats to clean JSON/REST/Kafka events.

## 2. Common Data Element (DE) Mapping

| DE | Name | Mapping in PayU | Description |
| :--- | :--- | :--- | :--- |
| **DE 2** | Primary Account Number (PAN) | `account.cardNumber` | Masked in logs (e.g., 4111********1111). |
| **DE 3** | Processing Code | `txn.type` | 00 (Purchase), 01 (Withdrawal), 20 (Refund). |
| **DE 4** | Amount, Transaction | `txn.amount` | Maps to BigDecimal in Ledger. |
| **DE 7** | Transmission Date/Time | `meta.timestamp` | UTC Format. |
| **DE 11** | System Trace Audit Number (STAN) | `meta.stan` | Crucial for 3-way reconciliation. |
| **DE 37** | Retrieval Reference Number (RRN) | `meta.rrn` | Primary key for external search. |
| **DE 39** | Response Code | `txn.responseCode` | 00 (Success), 51 (Insufficient Funds), etc. |
| **DE 48** | Additional Data | `meta.metadata` (JSONB) | Dynamic parsing for specific channel data. |

## 3. Implementation: The JPOS Adapter
PayU uses **jPOS** or a custom Netty-based ISO-parser.

### Parsing Pattern:
1. **Unpack**: Receive byte array, parse into `ISOMsg`.
2. **Translate**: Convert DE fields into `InternalTransactionRequest` DTO.
3. **Idempotency**: Use `DE 37 (RRN)` + `DE 11 (STAN)` as a unique key in Redis to prevent replay.
4. **ACK**: Return response code in `DE 39`.

## 4. Security & Compliance (PII)
- **Masking**: Never log DE 2 (PAN) or DE 35/45 (Track Data).
- **Encryption**: DE 52 (PIN Block) must only be handled inside the **HSM (Hardware Security Module)** or a secure HSM-Proxy.
- **Dumping**: If an ISO message fails parsing, dump the hex to a secure log file with automatic 7-day TTL.

---
*Last Updated: January 2026*
