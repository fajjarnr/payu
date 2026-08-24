package id.payu.partner.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.domain.PartnerStatus;

/**
 * ADR-0035 metrics: partner_onboarding_age_hours gauge + breach counters.
 */
@Component
public class PartnerOnboardingMetrics {

    private final MeterRegistry registry;
    private final Counter telegramEscalations;
    private final Counter pageEscalations;
    private final Counter slaBreaches;

    public PartnerOnboardingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.telegramEscalations = Counter.builder("payu.partner.onboarding.escalation.telegram")
                .description("Telegram escalations at T+4h").register(registry);
        this.pageEscalations = Counter.builder("payu.partner.onboarding.escalation.page")
                .description("Page escalations at T+24h").register(registry);
        this.slaBreaches = Counter.builder("payu.partner.onboarding.sla_breaches")
                .description("SLA breaches (>24h)").register(registry);
    }

    public void recordTelegramEscalation() { telegramEscalations.increment(); }
    public void recordPageEscalation() { pageEscalations.increment(); }
    public void recordSlaBreach() { slaBreaches.increment(); }

    public void recordTimeToDecision(Duration d) {
        registry.timer("payu.partner.onboarding.time_to_decision", "unit", "seconds").record(d);
    }

    public void recordPendingAgeHours(double hours) {
        registry.gauge("partner_onboarding_age_hours", hours);
    }
}
