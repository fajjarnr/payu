package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.PaymentLinkRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.PaymentLinkEntity;
import id.payu.partner.domain.PaymentLinkStatus;
import id.payu.partner.interfaces.dto.CreatePaymentLinkRequest;
import id.payu.partner.interfaces.dto.PaymentLinkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import id.payu.outbox.service.OutboxService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentLinkServiceTest {

    @Mock
    private PaymentLinkRepository paymentLinkRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private WebhookDispatcherService webhookDispatcher;

    @Mock
    private OutboxService outboxService;

    private PaymentLinkService paymentLinkService;

    private PartnerEntity activePartner;

    @BeforeEach
    void setUp() {
        paymentLinkService = new PaymentLinkService(paymentLinkRepository, partnerRepository, webhookDispatcher, outboxService);

        activePartner = new PartnerEntity("Test PartnerEntity", "MERCHANT", "test@partner.com", "08123456789", "api-key-1");
        activePartner.setId(1L);
        activePartner.setActive(true);
    }

    @Nested
    @DisplayName("createPaymentLink")
    class CreatePaymentLinkTests {

        @Test
        @DisplayName("should create payment link successfully")
        void shouldCreatePaymentLinkSuccessfully() {
            CreatePaymentLinkRequest request = new CreatePaymentLinkRequest();
            request.setAmount(new BigDecimal("150000.00"));
            request.setDescription("Invoice #INV-001");
            request.setCustomerName("John Doe");
            request.setCustomerEmail("john@example.com");
            request.setExpiryHours(48);

            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(paymentLinkRepository.save(any(PaymentLinkEntity.class)))
                    .thenAnswer(invocation -> {
                        PaymentLinkEntity saved = invocation.getArgument(0);
                        saved.setId(100L);
                        return saved;
                    });

            PaymentLinkResponse response = paymentLinkService.createPaymentLink(1L, request);

            assertNotNull(response);
            assertEquals(100L, response.getId());
            assertNotNull(response.getSlug());
            assertTrue(response.getPaymentUrl().contains(response.getSlug()));
            assertEquals(new BigDecimal("150000.00"), response.getAmount());
            assertEquals("IDR", response.getCurrency());
            assertEquals("Invoice #INV-001", response.getDescription());
            assertEquals("ACTIVE", response.getStatus());
            assertEquals("John Doe", response.getCustomerName());

            ArgumentCaptor<PaymentLinkEntity> captor = ArgumentCaptor.forClass(PaymentLinkEntity.class);
            verify(paymentLinkRepository).save(captor.capture());
            PaymentLinkEntity saved = captor.getValue();
            assertEquals(PaymentLinkStatus.ACTIVE, saved.getStatus());
            assertNotNull(saved.getExpiresAt());
        }

        @Test
        @DisplayName("should fail for non-existent partner")
        void shouldFailForNonExistentPartner() {
            when(partnerRepository.findById(999L)).thenReturn(Optional.empty());

            CreatePaymentLinkRequest request = new CreatePaymentLinkRequest();
            request.setAmount(BigDecimal.valueOf(50000));
            request.setDescription("Test");

            assertThrows(IllegalArgumentException.class,
                    () -> paymentLinkService.createPaymentLink(999L, request));
        }

        @Test
        @DisplayName("should fail for inactive partner")
        void shouldFailForInactivePartner() {
            PartnerEntity inactivePartner = new PartnerEntity("Inactive", "MERCHANT", "x@y.com", "08111", "key");
            inactivePartner.setId(2L);
            inactivePartner.setActive(false);

            when(partnerRepository.findById(2L)).thenReturn(Optional.of(inactivePartner));

            CreatePaymentLinkRequest request = new CreatePaymentLinkRequest();
            request.setAmount(BigDecimal.valueOf(50000));
            request.setDescription("Test");

            assertThrows(IllegalStateException.class,
                    () -> paymentLinkService.createPaymentLink(2L, request));
        }

        @Test
        @DisplayName("should fail for duplicate external ID")
        void shouldFailForDuplicateExternalId() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(paymentLinkRepository.existsByPartnerIdAndExternalId(1L, "order-123")).thenReturn(true);

            CreatePaymentLinkRequest request = new CreatePaymentLinkRequest();
            request.setAmount(BigDecimal.valueOf(50000));
            request.setDescription("Test");
            request.setExternalId("order-123");

            assertThrows(IllegalStateException.class,
                    () -> paymentLinkService.createPaymentLink(1L, request));
        }
    }

    @Nested
    @DisplayName("getBySlug")
    class GetBySlugTests {

        @Test
        @DisplayName("should return active payment link")
        void shouldReturnActivePaymentLink() {
            PaymentLinkEntity link = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().plusHours(24));
            link.setId(1L);

            when(paymentLinkRepository.findBySlug("abc123")).thenReturn(Optional.of(link));

            PaymentLinkResponse response = paymentLinkService.getBySlug("abc123");

            assertNotNull(response);
            assertEquals("ACTIVE", response.getStatus());
        }

        @Test
        @DisplayName("should auto-expire past-due link")
        void shouldAutoExpirePastDueLink() {
            PaymentLinkEntity link = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().minusHours(1));
            link.setId(1L);
            PaymentLinkEntity expired = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().minusHours(1));
            expired.setId(1L);
            expired.markExpired();

            when(paymentLinkRepository.findBySlug("expired123"))
                    .thenReturn(Optional.of(link), Optional.of(expired));
            when(paymentLinkRepository.markExpiredIfActive(1L)).thenReturn(1);

            PaymentLinkResponse response = paymentLinkService.getBySlug("expired123");

            assertEquals("EXPIRED", response.getStatus());
        }

        @Test
        @DisplayName("should fail for non-existent slug")
        void shouldFailForNonExistentSlug() {
            when(paymentLinkRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> paymentLinkService.getBySlug("nonexistent"));
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("should confirm payment successfully")
        void shouldConfirmPaymentSuccessfully() {
            PaymentLinkEntity link = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().plusHours(24));
            link.setId(1L);

            when(paymentLinkRepository.markPaidIfActive(eq("pay123"), any(LocalDateTime.class),
                    eq("WALLET"), eq("ref-001"))).thenReturn(1);
            when(paymentLinkRepository.findBySlug("pay123")).thenReturn(Optional.of(link));

            PaymentLinkResponse response = paymentLinkService.confirmPayment("pay123", "WALLET", "ref-001");

            assertEquals("PAID", response.getStatus());
            assertEquals("WALLET", response.getPaymentMethod());
            assertEquals("ref-001", response.getPaymentReference());
            assertNotNull(response.getPaidAt());
            verify(paymentLinkRepository, never()).save(any(PaymentLinkEntity.class));
        }

        @Test
        @DisplayName("double confirm on already-paid link is a deterministic no-op (IMP-2)")
        void shouldReturnExistingOnDoubleConfirm() {
            PaymentLinkEntity link = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().plusHours(24));
            link.setId(2L);
            PaymentLinkEntity paidLink = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().plusHours(24));
            paidLink.setId(2L);
            paidLink.markPaid("WALLET", "ref-001");

            when(paymentLinkRepository.markPaidIfActive(eq("pay123"), any(LocalDateTime.class),
                    eq("WALLET"), eq("ref-001"))).thenReturn(0);
            when(paymentLinkRepository.findBySlug("pay123"))
                    .thenReturn(Optional.of(link), Optional.of(paidLink));

            PaymentLinkResponse response = paymentLinkService.confirmPayment("pay123", "WALLET", "ref-001");

            assertEquals("PAID", response.getStatus());
            assertEquals("ref-001", response.getPaymentReference());
            verify(webhookDispatcher, never()).dispatch(anyString(), anyMap());
            verify(outboxService, never()).createEvent(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should fail for expired payment link")
        void shouldFailForExpiredPaymentLink() {
            PaymentLinkEntity link = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().minusHours(1));
            link.setId(1L);

            when(paymentLinkRepository.markPaidIfActive(eq("expired"), any(LocalDateTime.class),
                    eq("WALLET"), eq("ref-001"))).thenReturn(0);
            when(paymentLinkRepository.findBySlug("expired")).thenReturn(Optional.of(link));

            assertThrows(IllegalStateException.class,
                    () -> paymentLinkService.confirmPayment("expired", "WALLET", "ref-001"));
        }
    }

    @Nested
    @DisplayName("cancelPaymentLink")
    class CancelTests {

        @Test
        @DisplayName("should cancel active payment link")
        void shouldCancelActivePaymentLink() {
            PaymentLinkEntity link = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().plusHours(24));
            link.setId(10L);

            when(paymentLinkRepository.findById(10L)).thenReturn(Optional.of(link));

            assertDoesNotThrow(() -> paymentLinkService.cancelPaymentLink(1L, 10L));

            verify(paymentLinkRepository).save(any(PaymentLinkEntity.class));
        }

        @Test
        @DisplayName("should fail cancel for wrong partner")
        void shouldFailCancelForWrongPartner() {
            PartnerEntity otherPartner = new PartnerEntity("Other", "MERCHANT", "o@y.com", "08111", "key");
            otherPartner.setId(99L);

            PaymentLinkEntity link = new PaymentLinkEntity(otherPartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().plusHours(24));
            link.setId(10L);

            when(paymentLinkRepository.findById(10L)).thenReturn(Optional.of(link));

            assertThrows(IllegalArgumentException.class,
                    () -> paymentLinkService.cancelPaymentLink(1L, 10L));
        }
    }

    @Nested
    @DisplayName("listByPartner")
    class ListTests {

        @Test
        @DisplayName("should list payment links with pagination")
        void shouldListPaymentLinksWithPagination() {
            PaymentLinkEntity link1 = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Link 1", LocalDateTime.now().plusHours(24));
            link1.setId(1L);

            PageRequest pageable = PageRequest.of(0, 10);
            Page<PaymentLinkEntity> page = new PageImpl<>(List.of(link1), pageable, 1);

            when(paymentLinkRepository.findByPartnerId(eq(1L), any())).thenReturn(page);

            Page<PaymentLinkResponse> result = paymentLinkService.listByPartner(1L, pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals("Link 1", result.getContent().get(0).getDescription());
        }
    }

    @Nested
    @DisplayName("expirePaymentLinks")
    class ExpireTests {

        @Test
        @DisplayName("should expire past-due active links via conditional transition (IMP-2)")
        void shouldExpirePastDueActiveLinks() {
            PaymentLinkEntity link = new PaymentLinkEntity(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Expired", LocalDateTime.now().minusHours(1));
            link.setId(1L);

            when(paymentLinkRepository.findExpiredActiveLinks(any(LocalDateTime.class)))
                    .thenReturn(List.of(link));
            when(paymentLinkRepository.markExpiredIfActive(1L)).thenReturn(1);

            paymentLinkService.expirePaymentLinks();

            verify(paymentLinkRepository).markExpiredIfActive(1L);
            verify(paymentLinkRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should do nothing when no expired links")
        void shouldDoNothingWhenNoExpiredLinks() {
            when(paymentLinkRepository.findExpiredActiveLinks(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            paymentLinkService.expirePaymentLinks();

            verify(paymentLinkRepository, never()).saveAll(anyList());
        }
    }
}
