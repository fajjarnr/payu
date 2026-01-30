# Regulatory Reporting (BI & OJK)

## 1. Overview
As a financial institution, PayU must provide periodic data to Bank Indonesia (BI) and Otoritas Jasa Keuangan (OJK). Failure to report accurately or on time leads to heavy fines and license revocation.

## 2. Key Reporting Streams

### A. Antasena (BI)
Integrated reporting system for Bank Indonesia.
- **Scope**: Balance sheets, liquidity, interest rates, and foreign exchange.
- **format**: JSON/XML via secure API/Portal.
- **PayU Data Source**: `accounting-service` (General Ledger).

### B. SLIK (OJK)
Sistem Layanan Informasi Keuangan (Credit Bureau).
- **Scope**: Loan outstanding, collateral, and payment history (collectibility 1-5).
- **Format**: Fixed-length text files or specialized portal upload.
- **PayU Data Source**: `lending-service`.

### C. PPATK / AML
Suspicious Transaction Reporting.
- **Scope**: Transactions > Rp 500M or suspicious patterns (skimming, structuring).
- **Timeline**: Must be reported within 3 days of detection.
- **PayU Data Source**: `analytics-service` / Fraud Detection.

## 3. Reporting Architecture: The "Reporting Vault"
Microservices should NOT generate regulatory reports directly. They are too busy with OLTP.

**Pattern**:
1. Microservices stream data to **Data Lake / Reporting DB** (OLAP).
2. **Reporting Service** runs scheduled aggregations on OLAP.
3. Transformation scripts convert data into BI/OJK standardized schemas.
4. Final Review UI for Compliance Officer to "Sign-off" before transmission.

## 4. Data Lineage & PKS (Pusat Konsolidasi Sistem)
Regulators require "Data Lineage"—the ability to trace a number in a report back to the original database record.

- **Traceability**: Every report row must have a `source_system_ref`.
- **Integrity**: MD5/SHA-256 hashing of report files to ensure they weren't tampered with after generation.
