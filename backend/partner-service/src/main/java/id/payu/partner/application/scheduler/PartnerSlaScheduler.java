package id.payu.partner.application.scheduler;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.application.metrics.PartnerOnboardingMetrics;
import id.payu.partner.domain.PartnerStatus;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ADR-0035 SLA escalation: T+4h Telegram, T+24h page via outbox events.
 * ShedLock prevents GW-CONCUR-001 duplicate escalation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartnerSlaScheduler {

    private final PartnerRepository partnerRepository;
    private final PartnerOnboardingMetrics metrics;
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 900000) // 15 minutes (ADR-0035)
    @SchedulerLock(name = "PartnerSlaScheduler_checkSla", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")
    public void checkSla() {
        List<PartnerEntity> pending = partnerRepository.findByStatus(PartnerStatus.PENDING_APPROVAL);
        Instant now = Instant.now();
        for (PartnerEntity p : pending) {
            if (p.getRequestedAt() == null) continue;
            Duration age = Duration.between(p.getRequestedAt(), now);
            double hours = age.toMinutes() / 60.0;
            metrics.recordPendingAgeHours(hours);

            long ageHours = age.toHours();
            if (ageHours >= 24) {
                metrics.recordPageEscalation();
                metrics.recordSlaBreach();
                publishEscalation(p, "PAGE", ageHours);
            } else if (ageHours >= 4) {
                metrics.recordTelegramEscalation();
                publishEscalation(p, "TELEGRAM", ageHours);
            }
        }
    }

    private void publishEscalation(PartnerEntity p, String level, long ageHours) {
        String eventType = "PARTNER_SLA_" + level;
        String topic = level.equals("PAGE") ? "payu.partner.sla-page.v1" : "payu.partner.sla-telegram.v1";
        // fallback to valid topic pattern if needed: payu.partner.sla-page.v1 is already valid per pattern payu.<domain>.<event>.v<n>
        // payu.partner.sla-page.v1 -> domain=partner, event=sla-page valid.
        try {
            outboxService.createEvent(
                    "Partner", p.getId().toString(), eventType,
                    Map.of("partnerId", p.getId(), "makerId", p.getMakerId() != null ? p.getMakerId() : "unknown",
                            "ageHours", ageHours, "level", level, "requestedAt", p.getRequestedAt().toString()),
                    null, topic);
            log.warn("PARTNER_SLA_001 level={} partnerId={} ageHours={}", level, p.getId(), ageHours);
        } catch (Exception e) {
            log.error("Failed to publish SLA escalation event for partner {}: {}", p.getId(), e.getMessage());
        }
    }
}
