# Security Runbook - Incident Response for PayU Platform

> Security incident response procedures and guidelines for the PayU Digital Banking Platform

---

## Table of Contents

1. [Incident Severity Levels](#incident-severity-levels)
2. [Immediate Response Procedures](#immediate-response-procedures)
3. [Specific Incident Scenarios](#specific-incident-scenarios)
4. [Post-Incident Procedures](#post-incident-procedures)
5. [Security Monitoring Setup](#security-monitoring-setup)

---

## Incident Severity Levels

### Critical (P0) - Immediate Response Required (Within 15 minutes)

- Active data breach with customer PII exposure
- Production system compromise / unauthorized access
- Ransomware or active malware infection
- Large-scale fraud detection (multi-customer impact)
- Regulatory breach requiring immediate notification

### High (P1) - Response Within 1 Hour

- Suspicious security events requiring investigation
- Single customer account compromise
- Service disruption due to security controls
- Vulnerability exploit in production
- Data leak detected (containable)

### Medium (P2) - Response Within 4 Hours

- Security misconfiguration identified
- Non-critical vulnerability requiring patch
- Failed security controls (logging, monitoring)
- Suspicious authentication patterns

### Low (P3) - Response Within 24 Hours

- Security best practice violations
- Policy/procedure violations
- Minor vulnerabilities identified
- Security tool false positives

---

## Immediate Response Procedures

### 1. Incident Declaration

When a potential security incident is identified:

```bash
# Incident Command Checklist
[ ] Confirm the incident
[ ] Assign severity level (P0-P3)
[ ] Notify security team via Slack #security-incidents
[ ] Create incident ticket
[ ] Assign Incident Commander (IC)
[ ] Document initial observations
```

**Slack Command:**
```bash
/incident P0 "Potential data breach detected in account-service"
```

### 2. Initial Containment (P0/P1 Only)

**For Critical Incidents:**

```bash
# 1. Isolate affected systems
oc scale deployment account-service --replicas=0
oc scale deployment transaction-service --replicas=0

# 2. Enable maintenance mode
oc annotate route account-service maintenance="true"

# 3. Stop data exfiltration routes
oc patch networkpolicy deny-all-egress -p '{"spec":{"policyTypes":["Egress"]}}'

# 4. Enable enhanced logging
oc set env deployment/account-service LOG_LEVEL=DEBUG
```

### 3. Notification Matrix

| Severity | Internal Team | External Parties | Regulators | Customers |
|----------|--------------|------------------|------------|-----------|
| P0       | Immediately  | Immediately*     | <24h       | As required |
| P1       | Within 15m   | Within 1h        | <72h       | If affected |
| P2       | Within 1h    | As needed        | As required | No |
| P3       | Within 24h   | No               | No         | No |

*External parties may include law enforcement, security vendors, banks, payment processors

---

## Specific Incident Scenarios

### Scenario 1: Data Breach - Customer PII Exposure

**Detection Indicators:**
- Alert from DLP (Data Loss Prevention) system
- Unusual database export activity
- Customer reports of unauthorized account access
- Security findings from penetration testing

**Response Steps:**

1. **Immediate Actions (0-15 min)**
   ```bash
   # Identify scope
   oc get pods -l app=account-service -o wide
   oc logs --since=1h deployment/account-service > account-service-breach.log

   # Check for data exfiltration
   oc exec -it <pod-name> -- netstat -an | grep ESTABLISHED
   ```

2. **Containment (15-60 min)**
   - Rotate all database credentials
   - Revoke OAuth tokens
   - Force password reset for affected users
   - Enable enhanced monitoring

3. **Investigation (1-24 hours)**
   - Determine root cause
   - Identify affected customers
   - Calculate data exposure scope
   - Preserve forensic evidence

4. **Recovery (24-72 hours)**
   - Patch vulnerabilities
   - Restore from clean backups if needed
   - Notify affected parties
   - Implement additional security controls

### Scenario 2: Distributed Denial of Service (DDoS)

**Detection Indicators:**
- Spike in request rate (>10x normal)
- Service degradation
- High CPU/memory usage
- Multiple alerts from rate limiting

**Response Steps:**

1. **Immediate Actions**
   ```bash
   # Check current request rate
   oc top pods

   # Enable rate limiting (if not already active)
   oc apply -f infrastructure/openshift/rate-limit.yaml

   # Scale up services
   oc scale deployment account-service --replicas=10
   oc scale deployment transaction-service --replicas=10
   ```

2. **Activate CDN/DDoS Protection**
   - Enable Cloudflare/Incapsula DoS protection
   - Block geographic regions if attack is regional
   - Enable CAPTCHA challenges

3. **Post-Incident**
   - Analyze attack patterns
   - Update rate limiting rules
   - Implement additional protections

### Scenario 3: Authentication Bypass / Session Hijacking

**Detection Indicators:**
- Multiple failed logins from same IP
- Successful logins from unusual locations
- Session token anomalies
- Customer complaints of unauthorized access

**Response Steps:**

1. **Immediate Actions**
   ```bash
   # Revoke suspicious tokens
   oc exec -it <keycloak-pod> -- kcadm.sh revoke tokens \
     --users <affected-user-ids>

   # Enable MFA requirement
   oc set env deployment/auth-service REQUIRE_MFA=true

   # Check Keycloak logs
   oc logs deployment/keycloak --tail=1000 | grep -i error
   ```

2. **Investigation**
   - Identify compromised accounts
   - Check for credential stuffing patterns
   - Verify OAuth/OIDC flows
   - Review session management

3. **Recovery**
   - Force password resets
   - Implement IP-based access restrictions
   - Enable device fingerprinting
   - Update authentication policies

### Scenario 4: Circuit Breaker Failures (Cascading Outages)

**Detection Indicators:**
- Multiple circuit breakers in OPEN state
- Service degradation across multiple services
- High error rates in Resilience4j metrics

**Response Steps:**

1. **Immediate Actions**
   ```bash
   # Check circuit breaker status
   curl http://account-service:8081/actuator/health/circuitBreakers

   # Check Resilience4j metrics
   curl http://account-service:8081/actuator/metrics/resilience4j.circuitbreaker.state

   # Review recent deployments
   oc rollout history deployment/account-service
   ```

2. **Recovery Actions**
   - If recent deployment caused issues: rollback
   - If external service failing: enable fallback mode
   - If database issue: switch to read replica
   - Scale up affected services

3. **Prevent Recurrence**
   - Adjust circuit breaker thresholds
   - Implement bulkhead isolation
   - Add more aggressive retry policies
   - Improve external service timeout handling

---

## Post-Incident Procedures

### 1. Post-Mortem Report Template

```markdown
# Incident Post-Mortem: [INCIDENT_ID]

## Summary
- Date/Time: [Start - End]
- Duration: [X hours]
- Severity: [P0/P1/P2/P3]
- Incident Commander: [Name]

## Impact
- Affected Services: [List]
- Affected Customers: [Number/Percentage]
- Data Loss: [Yes/No - Details]
- Financial Impact: [Estimated]

## Root Cause Analysis
- What happened: [Description]
- How it happened: [Technical details]
- Why it happened: [Process/technical gaps]

## Timeline
| Time | Event | Action Taken |
|------|-------|-------------|
| HH:MM | Detection detected | [Actions] |
| HH:MM | Incident declared | [Actions] |
| ... | ... | ... |

## Resolution
- Immediate fixes applied: [List]
- Temporary workarounds: [List]
- Permanent fixes planned: [List]

## Lessons Learned
- What went well: [List]
- What could be improved: [List]
- Action items: [List with owners]
```

### 2. Action Items Tracking

All post-incident action items must be:
- Assigned to specific owners
- Added to the backlog (Jira/GitHub Issues)
- Tracked to completion
- Reviewed in weekly security standup

### 3. Knowledge Base Updates

After each incident:
- Update runbook with new scenarios
- Create playbooks for common issues
- Update monitoring dashboards
- Share learnings with team

---

## Security Monitoring Setup

### 1. Critical Security Metrics (Grafana)

```yaml
# Security Metrics Dashboard
metrics:
  - name: Failed Authentication Rate
    query: rate(auth_failed_total[5m])
    alert: > 100/min

  - name: Circuit Breaker Open
    query: resilience4j_circuitbreaker_state{state="open"}
    alert: Any circuit breaker open

  - name: High Rate of SQL Errors
    query: rate(hibernate_errors_total[5m])
    alert: > 10/min

  - name: Data Export Volume
    query: rate(api_bytes_exported[5m])
    alert: > 10x normal

  - name: Unusual Geographic Login
    query: geoip_distance_from_home{distance > 1000}
    alert: > 10 users
```

### 2. Alert Escalation Rules

| Alert Type | First Alert | Escalation 1 | Escalation 2 |
|------------|-------------|--------------|--------------|
| Security Incident | Slack #security | PagerDuty | CTO, CEO |
| Service Down | Slack #oncall | PagerDuty | Engineering Manager |
| Data Breach | Slack #security | PagerDuty | Legal, CEO |
| Fraud Detected | Slack #fraud | Fraud Team | Compliance Officer |

### 3. Automated Response Playbooks

```bash
# Example: Auto-containment script
#!/bin/bash
# scripts/auto-contain.sh

SERVICE=$1
INCIDENT_ID=$2

echo "Executing auto-containment for ${SERVICE} (Incident: ${INCIDENT_ID})"

# 1. Scale to zero
oc scale deployment/${SERVICE} --replicas=0

# 2. Enable maintenance mode
oc annotate route/${SERVICE} maintenance="true" \
  --overwrite

# 3. Notify Slack
curl -X POST $SLACK_WEBHOOK_URL \
  -H 'Content-Type: application/json' \
  -d "{\"text\": \"🚨 Auto-containment triggered for ${SERVICE}\nIncident: ${INCIDENT_ID}\"}"

# 4. Create Jira ticket
curl -X POST $JIRA_API_URL/rest/api/2/issue \
  -u $JIRA_CREDENTIALS \
  -d "{\"fields\":{\"project\":{\"key\":\"SEC\"},\"summary\":\"Auto-containment: ${SERVICE}\",\"issuetype\":{\"name\":\"Incident\"}}}"
```

---

## Contact Information

| Role | Name | Email | Phone | Slack |
|------|------|-------|-------|-------|
| CISO | [Name] | ciso@payu.id | +62-XXX | @ciso |
| Security Lead | [Name] | security-lead@payu.id | +62-XXX | @sec-lead |
| On-Call Engineer | [Rotating] | oncall@payu.id | +62-XXX | @oncall |
| Compliance Officer | [Name] | compliance@payu.id | +62-XXX | @compliance |
| Legal Counsel | [Name] | legal@payu.id | +62-XXX | @legal |

---

## Additional Resources

- [OJK Cybersecurity Guidelines](https://www.ojk.go.id)
- [PCI-DSS Requirements](https://www.pcisecuritystandards.org)
- [NIST Incident Response Guide](https://csrc.nist.gov/publications/detail/sp/800-61/rev-2/final)
- [OWASP Incident Response Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Incident_Response_Cheat_Sheet.html)

---

*Last Updated: January 2026*
*Version: 1.0.0*
*Maintained by: PayU Security Team*
