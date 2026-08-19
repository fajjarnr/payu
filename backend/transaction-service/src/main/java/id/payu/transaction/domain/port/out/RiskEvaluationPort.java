package id.payu.transaction.domain.port.out;

import java.math.BigDecimal;

/**
 * ADR-0030 + ADR-0036 + ADR-0038: Risk/AML fast-path (<30ms) — ponytail: stub returns 0, wire to analytics-service POST /api/v1/analytics/fraud/score when creds exist
 */
public interface RiskEvaluationPort {
    int score(String userId, BigDecimal amount, String currency);
}
