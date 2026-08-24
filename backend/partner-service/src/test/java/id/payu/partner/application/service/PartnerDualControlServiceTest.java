package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.domain.PartnerStatus;
import id.payu.partner.interfaces.dto.PartnerDTO;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.exception.ConflictException;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD for ADR-0035: maker≠checker, status transitions, outbox, SLA semantics.
 */
@ExtendWith(MockitoExtension.class)
public class PartnerDualControlServiceTest {

    @Mock PartnerRepository partnerRepository;
    @Mock OutboxService outboxService;
    @InjectMocks PartnerService partnerService;

    private PartnerDTO newPartnerDto;

    @BeforeEach
    void setUp() {
        newPartnerDto = new PartnerDTO(null, "TokoBapak", "SNAP_BI", "ops@tokobapak.id", "+62123456789", false, null, null, null);
    }

    @Test
    void createPartner_setsPendingApproval_andMaker_andOutbox() {
        when(partnerRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(partnerRepository.save(any())).thenAnswer(i -> { PartnerEntity p = i.getArgument(0); p.setId(1L); return p; });

        PartnerDTO result = partnerService.createPartner(newPartnerDto, "maker-1");

        assertEquals("PENDING_APPROVAL", result.status);
        assertEquals("maker-1", result.makerId);
        assertNotNull(result.requestedAt);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).createEvent(eq("Partner"), eq("1"), eq("PartnerOnboardingRequested"), anyMap(), isNull(), topicCaptor.capture());
        assertEquals("payu.partner.onboarding-requested.v1", topicCaptor.getValue());
    }

    @Test
    void createPartner_internalBypassesDualControl_andNoOutbox() {
        newPartnerDto.type = "INTERNAL";
        when(partnerRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(partnerRepository.save(any())).thenAnswer(i -> { PartnerEntity p = i.getArgument(0); p.setId(2L); return p; });

        PartnerDTO result = partnerService.createPartner(newPartnerDto, "maker-1");

        assertEquals("ACTIVE", result.status);
        verify(outboxService, never()).createEvent(any(), any(), any(), anyMap(), any(), any());
    }

    @Test
    void createPartner_sandboxBypasses() {
        newPartnerDto.type = "SANDBOX";
        when(partnerRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(partnerRepository.save(any())).thenAnswer(i -> { PartnerEntity p = i.getArgument(0); p.setId(3L); return p; });
        PartnerDTO result = partnerService.createPartner(newPartnerDto, "maker-1");
        assertEquals("ACTIVE", result.status);
    }

    @Test
    void approve_pendingApproval_makerNotEqualChecker_transitionsToActive_andOutbox() {
        PartnerEntity pending = pendingApproval("maker-1");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(partnerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PartnerDTO result = partnerService.approvePartner(1L, "checker-1");

        assertEquals("ACTIVE", result.status);
        assertEquals("checker-1", result.checkerId);
        assertNotNull(result.decidedAt);
        verify(outboxService).createEvent(eq("Partner"), eq("1"), eq("PartnerApproved"), anyMap(), isNull(), eq("payu.partner.approved.v1"));
    }

    @Test
    void approve_selfApproval_throwsForbidden() {
        PartnerEntity pending = pendingApproval("maker-1");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(pending));

        BusinessException ex = assertThrows(BusinessException.class, () -> partnerService.approvePartner(1L, "maker-1"));
        assertEquals("PARTNER_FORBIDDEN_SELF_APPROVAL", ex.getCode());
        verify(outboxService, never()).createEvent(any(), any(), any(), anyMap(), any(), any());
    }

    @Test
    void approve_wrongStatus_throwsConflict() {
        PartnerEntity active = pendingApproval("maker-1");
        active.setStatus(PartnerStatus.ACTIVE);
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(active));
        assertThrows(ConflictException.class, () -> partnerService.approvePartner(1L, "checker-1"));
    }

    @Test
    void reject_setsRejected_andReason_andOutbox() {
        PartnerEntity pending = pendingApproval("maker-1");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(partnerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PartnerDTO result = partnerService.rejectPartner(1L, "checker-1", "docs incomplete");

        assertEquals("REJECTED", result.status);
        assertEquals("docs incomplete", result.rejectionReason);
        verify(outboxService).createEvent(eq("Partner"), eq("1"), eq("PartnerRejected"), anyMap(), isNull(), eq("payu.partner.rejected.v1"));
    }

    @Test
    void reject_selfReject_throwsForbidden() {
        PartnerEntity pending = pendingApproval("maker-1");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(pending));
        BusinessException ex = assertThrows(BusinessException.class, () -> partnerService.rejectPartner(1L, "maker-1", "bad"));
        assertEquals("PARTNER_FORBIDDEN_SELF_APPROVAL", ex.getCode());
    }

    @Test
    void reject_missingReason_throws() {
        PartnerEntity pending = pendingApproval("maker-1");
        assertThrows(BusinessException.class, () -> partnerService.rejectPartner(1L, "checker-1", " "));
        assertThrows(BusinessException.class, () -> partnerService.rejectPartner(1L, "checker-1", null));
    }

    @Test
    void resubmit_rejected_toPendingApproval_clearsChecker_andOutbox() {
        PartnerEntity rejected = new PartnerEntity();
        rejected.setId(1L);
        rejected.setName("Toko"); rejected.setType("SNAP_BI"); rejected.setEmail("x@y.id");
        rejected.setStatus(PartnerStatus.REJECTED);
        rejected.setMakerId("maker-1"); rejected.setCheckerId("checker-1");
        rejected.setRequestedAt(Instant.now().minusSeconds(3600));
        rejected.setDecidedAt(Instant.now()); rejected.setRejectionReason("bad");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(rejected));
        when(partnerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PartnerDTO result = partnerService.resubmitPartner(1L, "maker-1");

        assertEquals("PENDING_APPROVAL", result.status);
        assertNull(result.checkerId);
        assertNull(result.rejectionReason);
        assertNotNull(result.requestedAt);
        assertNull(result.decidedAt);
        verify(outboxService).createEvent(eq("Partner"), eq("1"), eq("PartnerOnboardingRequested"), anyMap(), isNull(), eq("payu.partner.onboarding-requested.v1"));
    }

    @Test
    void resubmit_wrongStatus_throwsConflict() {
        PartnerEntity pending = pendingApproval("maker-1");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(pending));
        assertThrows(ConflictException.class, () -> partnerService.resubmitPartner(1L, "maker-1"));
    }

    @Test
    void delete_onlyRejected_allowed() {
        PartnerEntity active = pendingApproval("maker-1");
        active.setStatus(PartnerStatus.ACTIVE);
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(active));
        assertThrows(ConflictException.class, () -> partnerService.deletePartner(1L));

        PartnerEntity rejected = pendingApproval("maker-1");
        rejected.setStatus(PartnerStatus.REJECTED);
        when(partnerRepository.findById(2L)).thenReturn(Optional.of(rejected));
        assertTrue(partnerService.deletePartner(2L));
        verify(partnerRepository).deleteById(2L);
    }

    private PartnerEntity pendingApproval(String makerId) {
        PartnerEntity p = new PartnerEntity();
        p.setId(1L);
        p.setName("Toko"); p.setType("SNAP_BI"); p.setEmail("ops@tokobapak.id");
        p.setStatus(PartnerStatus.PENDING_APPROVAL);
        p.setMakerId(makerId);
        p.setRequestedAt(Instant.now().minusSeconds(60));
        return p;
    }
}
