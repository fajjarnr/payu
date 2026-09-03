package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.outbox.service.OutboxService;
import id.payu.security.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PAYU-TB-006: pre-auth partner lookup must run SYSTEM-scoped (FORCE RLS
 * hides every partners row when no tenant is bound) and must restore the
 * previous tenant afterwards so request handling keeps its own scope.
 */
@ExtendWith(MockitoExtension.class)
class PartnerServiceAuthLookupTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PartnerService partnerService;

    private PartnerEntity partner;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        partner = new PartnerEntity();
        partner.setId(7L);
        partner.setClientId("tokobapak-mvp");
        partner.setTenantId("tokobapak");
        partner.setActive(true);
        partner.setStatus(id.payu.partner.domain.PartnerStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("auth lookup returns partner and restores unset tenant")
    void returnsPartnerAndRestoresUnset() {
        when(partnerRepository.findByClientId("tokobapak-mvp")).thenReturn(Optional.of(partner));

        Optional<PartnerEntity> found = partnerService.findByClientIdForAuth("tokobapak-mvp");

        assertTrue(found.isPresent());
        assertEquals("tokobapak", found.get().getTenantId());
        verify(partnerRepository).findByClientId("tokobapak-mvp");
        assertFalse(TenantContext.isSet(), "previous unset tenant must be restored");
    }

    @Test
    @DisplayName("auth lookup restores previously set tenant")
    void restoresPreviousTenant() {
        when(partnerRepository.findByClientId("tokobapak-mvp")).thenReturn(Optional.of(partner));
        TenantContext.setTenantId("other-tenant");

        partnerService.findByClientIdForAuth("tokobapak-mvp");

        assertEquals("other-tenant", TenantContext.getTenantId());
    }

    @Test
    @DisplayName("auth lookup propagates empty and still restores tenant")
    void emptyRestoresTenant() {
        when(partnerRepository.findByClientId("unknown")).thenReturn(Optional.empty());
        TenantContext.setTenantId("other-tenant");

        assertTrue(partnerService.findByClientIdForAuth("unknown").isEmpty());
        assertEquals("other-tenant", TenantContext.getTenantId());
    }
}
