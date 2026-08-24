package id.payu.partner.application.scheduler;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.application.metrics.PartnerOnboardingMetrics;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.domain.PartnerStatus;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartnerSlaSchedulerTest {

    @Mock PartnerRepository partnerRepository;
    @Mock PartnerOnboardingMetrics metrics;
    @Mock OutboxService outboxService;
    @InjectMocks PartnerSlaScheduler scheduler;

    @Test
    void tPlus4h_triggersTelegramEscalation() {
        PartnerEntity p = pendingWithAgeHours(5);
        when(partnerRepository.findByStatus(PartnerStatus.PENDING_APPROVAL)).thenReturn(List.of(p));

        scheduler.checkSla();

        verify(metrics).recordTelegramEscalation();
        verify(outboxService).createEvent(eq("Partner"), eq("1"), eq("PARTNER_SLA_TELEGRAM"), anyMap(), isNull(), eq("payu.partner.sla-telegram.v1"));
        verify(metrics, never()).recordPageEscalation();
    }

    @Test
    void tPlus24h_triggersPageEscalation() {
        PartnerEntity p = pendingWithAgeHours(25);
        when(partnerRepository.findByStatus(PartnerStatus.PENDING_APPROVAL)).thenReturn(List.of(p));

        scheduler.checkSla();

        verify(metrics).recordPageEscalation();
        verify(outboxService).createEvent(eq("Partner"), eq("1"), eq("PARTNER_SLA_PAGE"), anyMap(), isNull(), eq("payu.partner.sla-page.v1"));
    }

    @Test
    void under4h_noEscalation() {
        PartnerEntity p = pendingWithAgeHours(2);
        when(partnerRepository.findByStatus(PartnerStatus.PENDING_APPROVAL)).thenReturn(List.of(p));
        scheduler.checkSla();
        verify(metrics, never()).recordTelegramEscalation();
        verify(metrics, never()).recordPageEscalation();
        verify(outboxService, never()).createEvent(any(), any(), any(), anyMap(), any(), any());
    }

    @Test
    void hasShedLockAnnotation() throws Exception {
        var m = PartnerSlaScheduler.class.getMethod("checkSla");
        assert m.isAnnotationPresent(net.javacrumbs.shedlock.spring.annotation.SchedulerLock.class);
        assert m.isAnnotationPresent(org.springframework.scheduling.annotation.Scheduled.class);
    }

    private PartnerEntity pendingWithAgeHours(long hours) {
        PartnerEntity p = new PartnerEntity();
        p.setId(1L); p.setName("Toko"); p.setType("SNAP_BI"); p.setEmail("ops@tokobapak.id");
        p.setStatus(PartnerStatus.PENDING_APPROVAL);
        p.setMakerId("maker-1");
        p.setRequestedAt(Instant.now().minusSeconds(hours * 3600));
        return p;
    }
}
