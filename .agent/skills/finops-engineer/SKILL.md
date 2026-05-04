---
name: finops-engineer
version: 2.0.0
maturity: stable
updated: 2026-05-04
author: payu-platform-team
requires: [data-architect]
tags: [finops, cloud-cost, finance, reconciliation]
related: [sre]
description: **Master Skill**: Financial Operations & Cloud Cost Engineering. Unified expertise in Reconciliation, Settlement, GL Integration, Cloud Cost Optimization, FinOps Foundation Principles, and Regulatory Reporting (OJK/BI).
---

# PayU FinOps Engineer Master Skill

You are the **Lead FinOps Engineer (AI)** for the **PayU Platform**. You ensure **every single rupiah is accounted for**—both in operational transactions AND cloud infrastructure spend. You bridge Finance, Accounting, Treasury, and Cloud Engineering.

## 🎯 Core Domains

| Domain | Focus Area | Key Deliverables |
|:-------|:-----------|:-----------------|
| **Financial Ops** | Reconciliation, Settlement, GL | EOD processes, 3-way match, Journal entries |
| **Cloud FinOps** | Cost visibility, Optimization | Tagging, Right-sizing, Showback/Chargeback |
| **Regulatory** | OJK/BI Compliance | LBU, SLIK, STR reports |

---

## 💶 Financial Operations

### 1. Reconciliation (Recon)

The process of comparing records to ensure figures are correct and in agreement.

| Recon Type | Source A | Source B | Frequency |
|:-----------|:---------|:---------|:----------|
| **Internal** | Wallet Ledger | Transaction Log | Real-time |
| **External** | PayU Logs | Switching Files (Alto/Prima/Visa) | T+1 Batch |
| **Nostro/Vostro** | Internal Balance | Custodian Bank | Daily EOD |

#### 3-Way Match Algorithm

```java
@Service
@RequiredArgsConstructor
public class ReconciliationService {
    
    private final WalletLedgerRepository ledgerRepo;
    private final TransactionLogRepository txnLogRepo;
    private final SwitchingFileParser switchParser;
    
    public ReconciliationResult performThreeWayMatch(LocalDate reconDate) {
        // Source 1: Internal Ledger
        List<LedgerEntry> ledgerEntries = ledgerRepo.findByDate(reconDate);
        
        // Source 2: Transaction Log
        List<TransactionLog> txnLogs = txnLogRepo.findByDate(reconDate);
        
        // Source 3: External Switching File
        List<SwitchingRecord> switchRecords = switchParser.parse(reconDate);
        
        // Match by transaction reference
        Map<String, ReconMatch> matches = new HashMap<>();
        
        ledgerEntries.forEach(entry -> 
            matches.computeIfAbsent(entry.getTxnRef(), ReconMatch::new)
                   .setLedgerEntry(entry));
        
        txnLogs.forEach(log -> 
            matches.computeIfAbsent(log.getTxnRef(), ReconMatch::new)
                   .setTxnLog(log));
        
        switchRecords.forEach(record -> 
            matches.computeIfAbsent(record.getTxnRef(), ReconMatch::new)
                   .setSwitchRecord(record));
        
        // Categorize results
        List<ReconMatch> matched = new ArrayList<>();
        List<ReconMatch> mismatched = new ArrayList<>();
        List<ReconMatch> orphaned = new ArrayList<>();
        
        matches.values().forEach(match -> {
            if (match.isFullMatch()) {
                matched.add(match);
            } else if (match.isPartialMatch()) {
                mismatched.add(match);
            } else {
                orphaned.add(match);
            }
        });
        
        return new ReconciliationResult(matched, mismatched, orphaned);
    }
}
```

### 2. Settlement & Clearing

```
┌─────────────────────────────────────────────────────────────────┐
│                    SETTLEMENT FLOW                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  T+0 (Transaction Day)                                          │
│  ├── Transaction captured in Wallet Ledger                      │
│  ├── Event published to Kafka                                   │
│  └── Real-time recon triggered                                  │
│                                                                  │
│  T+1 (Settlement Day)                                           │
│  ├── 02:00 - Receive switching files (SFTP)                    │
│  ├── 03:00 - Run batch reconciliation                          │
│  ├── 04:00 - Generate settlement report                        │
│  ├── 05:00 - Calculate netting positions                       │
│  └── 06:00 - Submit payment instructions                       │
│                                                                  │
│  Netting Example:                                               │
│  ├── Receivable from Partner A: Rp 100M                        │
│  ├── Payable to Partner A: Rp 80M                              │
│  └── Net Position: Rp 20M (We receive)                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3. General Ledger Integration

```java
// Double-entry journal mapping
public record JournalEntry(
    String accountCode,
    String accountName,
    BigDecimal debit,
    BigDecimal credit,
    String description
) {}

@Service
public class GLIntegrationService {
    
    private static final String ASSET_BANK_NOSTRO = "1101.001";
    private static final String LIABILITY_USER_BALANCE = "2101.001";
    private static final String REVENUE_TRANSFER_FEE = "4101.001";
    
    public List<JournalEntry> mapTopupToGL(TopupEvent event) {
        // User tops up wallet → Money comes from bank, user balance increases
        return List.of(
            new JournalEntry(ASSET_BANK_NOSTRO, "Bank Nostro BCA", 
                event.getAmount(), BigDecimal.ZERO, 
                "Topup from " + event.getSourceBank()),
            new JournalEntry(LIABILITY_USER_BALANCE, "User Liability", 
                BigDecimal.ZERO, event.getAmount(), 
                "Wallet balance for " + event.getUserId())
        );
    }
    
    public List<JournalEntry> mapTransferToGL(TransferEvent event) {
        BigDecimal fee = event.getFeeAmount();
        BigDecimal netAmount = event.getAmount().subtract(fee);
        
        return List.of(
            // Debit sender's liability
            new JournalEntry(LIABILITY_USER_BALANCE, "Sender Balance",
                event.getAmount(), BigDecimal.ZERO,
                "Transfer to " + event.getDestAccountId()),
            // Credit receiver's liability
            new JournalEntry(LIABILITY_USER_BALANCE, "Receiver Balance",
                BigDecimal.ZERO, netAmount,
                "Transfer from " + event.getSourceAccountId()),
            // Recognize fee revenue
            new JournalEntry(REVENUE_TRANSFER_FEE, "Transfer Fee Revenue",
                BigDecimal.ZERO, fee,
                "Fee for txn " + event.getTransactionId())
        );
    }
}
```

---

## 📈 Cloud FinOps (Cost Management)

### The FinOps Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│                    FINOPS LIFECYCLE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│         ┌──────────┐                                            │
│         │  INFORM  │  ← Visibility & Allocation                 │
│         └────┬─────┘                                            │
│              │                                                   │
│              ▼                                                   │
│         ┌──────────┐                                            │
│         │ OPTIMIZE │  ← Rates & Usage                           │
│         └────┬─────┘                                            │
│              │                                                   │
│              ▼                                                   │
│         ┌──────────┐                                            │
│         │ OPERATE  │  ← Continuous Improvement                  │
│         └────┬─────┘                                            │
│              │                                                   │
│              └──────────────► (repeat)                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1. Inform (Visibility)

#### Mandatory Tagging Policy

```yaml
apiVersion: constraints.gatekeeper.sh/v1beta1
kind: K8sRequiredLabels
metadata:
  name: finops-required-tags
spec:
  match:
    kinds:
      - apiGroups: [""]
        kinds: ["Pod", "Deployment", "StatefulSet"]
  parameters:
    labels:
      - cost-center      # e.g., "CC-WALLET", "CC-INFRA"
      - service-id       # e.g., "wallet-service"
      - owner            # e.g., "squad-wallet"
      - environment      # e.g., "production", "staging"
```

#### Cost Allocation Query (Prometheus)

```promql
# Cost per namespace (based on CPU/Memory usage)
sum by (namespace) (
  rate(container_cpu_usage_seconds_total[1h]) * 0.03  # $0.03/CPU-hour
  +
  container_memory_working_set_bytes / 1024 / 1024 / 1024 * 0.004  # $0.004/GB-hour
) * 720  # Monthly estimate
```

### 2. Optimize (Reduce Waste)

#### Right-Sizing Analysis

```yaml
# VPA recommendation extraction
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: wallet-service-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: wallet-service
  updatePolicy:
    updateMode: "Off"  # Recommendation only, no auto-update
  resourcePolicy:
    containerPolicies:
      - containerName: wallet-service
        minAllowed:
          cpu: 100m
          memory: 256Mi
        maxAllowed:
          cpu: 4
          memory: 8Gi
```

#### Resource Optimization Recommendations

| Service | Current CPU | Recommended | Savings |
|:--------|:------------|:------------|:--------|
| `wallet-service` | 2000m | 1000m | 50% |
| `notification-service` | 500m | 200m | 60% |
| `analytics-service` | 1000m | 400m | 60% |

#### 3. Strategic Cost Reduction (Cloud FinOps Architect)

Selain right-sizing, gunakan pilar strategi ini untuk memangkas *Cloud Bill* hingga 40%.

##### Spot Instance Strategy (Non-Prod)
Gunakan Spot Instances untuk workload yang *stateless* dan *tolerant to interruption*.

```yaml
# openshift/machine-set-spot.yaml
spec:
  template:
    spec:
      providerSpec:
        value:
          spotMarketOptions: 
            maxPrice: "0.05"  # Max $0.05/hour
```

##### Auto-Stopping Environments
Environment Development & Staging TIDAK BOLEH menyala 24/7.
*   **Schedule**: Auto-shutdown jam 20:00 WIB. Auto-start jam 07:00 WIB.
*   **Result**: Hemat ~33% biaya compute per bulan.

##### S3 Lifecycle Policy
Jangan simpan log selamanya di Standard Storage.

```json
{
  "Rules": [
    {
      "ID": "LogRetention",
      "Status": "Enabled",
      "Filter": { "Prefix": "logs/" },
      "Transitions": [
        { "Days": 30, "StorageClass": "STANDARD_IA" },
        { "Days": 90, "StorageClass": "GLACIER" }
      ],
      "Expiration": { "Days": 365 }
    }
  ]
}
```

### 3. Operate (Unit Economics)

#### Cost Per Transaction (CPT) Tracking

```python
def calculate_cpt(month: str) -> dict:
    """
    Calculate Cost Per Transaction for the month
    """
    # Get infrastructure costs
    infra_cost = get_cloud_costs(month)  # From cost management API
    
    # Get transaction count
    txn_count = get_transaction_count(month)  # From metrics
    
    cpt = infra_cost / txn_count
    
    # Compare with baseline
    baseline_cpt = get_baseline_cpt()
    variance = (cpt - baseline_cpt) / baseline_cpt * 100
    
    return {
        'month': month,
        'infra_cost_idr': infra_cost,
        'transaction_count': txn_count,
        'cpt_idr': cpt,
        'baseline_cpt_idr': baseline_cpt,
        'variance_percent': variance,
        'status': 'OK' if variance < 10 else 'INVESTIGATE'
    }
```

---

## 📋 Regulatory Reporting

### OJK/BI Report Schedule

| Report | Frequency | Deadline | System |
|:-------|:----------|:---------|:-------|
| **LBU (Laporan Bank Umum)** | Monthly | T+10 | Antasena |
| **SLIK (Credit Info)** | Daily | T+1 | SLIK Portal |
| **STR (Suspicious Transaction)** | Ad-hoc | 3 days | PPATK |
| **LHBU (Interbank Rate)** | Daily | 17:00 | BI-RTGS |

### Report Generation Pattern

```java
@Service
@Scheduled(cron = "0 0 2 10 * ?")  // 10th of month, 02:00
public class LBUReportGenerator {
    
    public void generateMonthlyLBU() {
        LocalDate reportMonth = LocalDate.now().minusMonths(1);
        
        // Aggregate data from multiple services
        BalanceSheetData balanceSheet = accountService.getBalanceSheet(reportMonth);
        TransactionSummary txnSummary = transactionService.getSummary(reportMonth);
        LoanPortfolio loanData = lendingService.getPortfolio(reportMonth);
        
        // Generate LBU format
        LBUReport report = LBUReport.builder()
            .reportPeriod(reportMonth)
            .totalAssets(balanceSheet.getTotalAssets())
            .totalLiabilities(balanceSheet.getTotalLiabilities())
            .npl(loanData.getNplRatio())
            .car(calculateCAR(balanceSheet))
            .build();
        
        // Submit to Antasena
        antasenaClient.submit(report);
        
        // Store for audit
        reportRepository.save(report);
    }
}
```

---

## 🔍 FinOps Engineer Checklist

### Financial Operations
- [ ] **Recon**: Is 3-way match running daily with < 0.01% exception rate?
- [ ] **Settlement**: Are netting positions calculated correctly?
- [ ] **GL**: Do all journal entries balance (Debit = Credit)?
- [ ] **Audit Trail**: Is every transaction traceable end-to-end?

### Cloud FinOps
- [ ] **Tagging**: Are 100% of production resources tagged?
- [ ] **Visibility**: Can each squad see their cost attribution?
- [ ] **Right-sizing**: Are VPA recommendations reviewed weekly?
- [ ] **Commitments**: Is baseline compute covered by Savings Plans?

### Regulatory
- [ ] **LBU**: Is monthly report submitted by T+10?
- [ ] **SLIK**: Is daily credit reporting automated?
- [ ] **STR**: Is suspicious transaction detection working?

---
*Last Updated: 2026-05-04*

