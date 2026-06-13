# FinOps Tagging Strategy

## Overview

Strategi resource tagging untuk cost allocation, chargeback, dan cloud cost optimization di PayU.

---

## 🏷️ Mandatory Tags

Semua cloud resources **WAJIB** memiliki tags berikut:

| Tag Key | Description | Example Values | Required |
|---------|-------------|----------------|----------|
| `payu:environment` | Deployment environment | `prod`, `staging`, `dev`, `dr` | ✅ Yes |
| `payu:service` | Service name | `wallet-service`, `auth-service` | ✅ Yes |
| `payu:team` | Owning team | `platform`, `payments`, `lending` | ✅ Yes |
| `payu:cost-center` | Cost allocation code | `CC-PAYMENTS-001`, `CC-INFRA-002` | ✅ Yes |
| `payu:product` | Business product | `digital-banking`, `paylater`, `investment` | ✅ Yes |

---

## 📋 Tag Taxonomy

### Environment Tags

```yaml
payu:environment:
  - prod          # Production workloads
  - staging       # Pre-production testing
  - dev           # Development/sandbox
  - dr            # Disaster recovery
  - perf          # Performance testing
```

### Service Tags (Microservices)

```yaml
payu:service:
  # Core Banking
  - account-service
  - wallet-service
  - transaction-service
  - auth-service
  
  # Value-Added
  - investment-service
  - lending-service
  - billing-service
  
  # Supporting
  - notification-service
  - analytics-service
  - gateway-service
```

### Team Tags

```yaml
payu:team:
  - platform       # Infrastructure & DevOps
  - payments       # Core payment processing
  - lending        # Lending & credit products
  - investment     # Investment products
  - mobile         # Mobile applications
  - web            # Web applications
  - data           # Data engineering & analytics
  - security       # Security operations
```

### Cost Center Mapping

```yaml
cost_centers:
  # Engineering
  CC-PLATFORM-001: "Platform Infrastructure"
  CC-PAYMENTS-001: "Payment Processing"
  CC-LENDING-001: "Lending Products"
  CC-INVESTMENT-001: "Investment Products"
  
  # Shared Services
  CC-SHARED-001: "Shared Infrastructure"
  CC-SECURITY-001: "Security Operations"
  CC-DATA-001: "Data & Analytics"
  
  # Non-Production
  CC-DEV-001: "Development Environments"
  CC-QA-001: "Quality Assurance"
```

---

## 🔧 Implementation

### Kubernetes Labels

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wallet-service
  labels:
    payu.fajjjar.my.id/environment: prod
    payu.fajjjar.my.id/service: wallet-service
    payu.fajjjar.my.id/team: payments
    payu.fajjjar.my.id/cost-center: CC-PAYMENTS-001
    payu.fajjjar.my.id/product: digital-banking
spec:
  template:
    metadata:
      labels:
        payu.fajjjar.my.id/environment: prod
        payu.fajjjar.my.id/service: wallet-service
```

### Terraform

```hcl
# modules/common/tags.tf
locals {
  common_tags = {
    "payu:environment" = var.environment
    "payu:service"     = var.service_name
    "payu:team"        = var.team
    "payu:cost-center" = var.cost_center
    "payu:product"     = var.product
    "payu:managed-by"  = "terraform"
    "payu:created-at"  = timestamp()
  }
}

# Usage
resource "aws_instance" "app" {
  # ...
  tags = merge(local.common_tags, {
    Name = "${var.service_name}-${var.environment}"
  })
}
```

### AWS CloudFormation

```yaml
Parameters:
  Environment:
    Type: String
    AllowedValues: [prod, staging, dev]
  ServiceName:
    Type: String
  Team:
    Type: String
  CostCenter:
    Type: String

Resources:
  AppInstance:
    Type: AWS::EC2::Instance
    Properties:
      Tags:
        - Key: payu:environment
          Value: !Ref Environment
        - Key: payu:service
          Value: !Ref ServiceName
        - Key: payu:team
          Value: !Ref Team
        - Key: payu:cost-center
          Value: !Ref CostCenter
```

---

## 📊 Cost Allocation Reports

### By Team (Monthly)

```sql
-- AWS Cost Explorer / BigQuery
SELECT 
  tags['payu:team'] as team,
  SUM(cost) as total_cost,
  SUM(cost) / SUM(SUM(cost)) OVER() * 100 as percentage
FROM cloud_costs
WHERE month = '2026-01'
GROUP BY tags['payu:team']
ORDER BY total_cost DESC;
```

### By Service (Weekly)

```sql
SELECT 
  tags['payu:service'] as service,
  tags['payu:environment'] as env,
  SUM(cost) as weekly_cost,
  LAG(SUM(cost)) OVER (PARTITION BY tags['payu:service'] ORDER BY week) as prev_week,
  (SUM(cost) - LAG(SUM(cost)) OVER (...)) / LAG(SUM(cost)) OVER (...) * 100 as wow_change
FROM cloud_costs
WHERE week = CURRENT_WEEK()
GROUP BY 1, 2;
```

---

## 🚨 Tag Compliance

### Enforcement Policy

```yaml
# OPA Policy: Deny untagged resources
package kubernetes.admission

deny[msg] {
  required_tags := ["payu.fajjjar.my.id/environment", "payu.fajjjar.my.id/service", "payu.fajjjar.my.id/team", "payu.fajjjar.my.id/cost-center"]
  provided_tags := {tag | input.request.object.metadata.labels[tag]}
  missing := required_tags - provided_tags
  count(missing) > 0
  msg := sprintf("Missing required tags: %v", [missing])
}
```

### Compliance Dashboard

| Metric | Target | Current |
|--------|--------|---------|
| Tagged Resources | 100% | 98.5% |
| Valid Cost Centers | 100% | 99.2% |
| Orphaned Resources | 0 | 12 |
| Untagged Spend | $0 | $1,250/month |

---

## 💰 Chargeback Model

### Allocation Rules

```yaml
chargeback_rules:
  # Direct allocation (90% of costs)
  direct:
    - condition: "tags['payu:cost-center'] != null"
      action: "allocate_to_cost_center"
  
  # Shared services (10% of costs)
  shared:
    - service: "gateway-service"
      allocation: "proportional_to_traffic"
    
    - service: "kafka-cluster"
      allocation: "proportional_to_messages"
    
    - service: "monitoring-stack"
      allocation: "equal_split_all_teams"
```

### Monthly Chargeback Report

| Team | Direct Costs | Shared Costs | Total | Budget | Variance |
|------|--------------|--------------|-------|--------|----------|
| Platform | $45,000 | $15,000 | $60,000 | $65,000 | -$5,000 ✅ |
| Payments | $120,000 | $25,000 | $145,000 | $140,000 | +$5,000 ⚠️ |
| Lending | $35,000 | $10,000 | $45,000 | $50,000 | -$5,000 ✅ |

---

## 🔄 Tag Governance

### Quarterly Review

- [ ] Validate cost center mappings
- [ ] Remove obsolete service tags
- [ ] Update team ownership
- [ ] Reconcile unallocated costs
- [ ] Generate chargeback reports
