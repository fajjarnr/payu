package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.PaymentLinkRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.domain.Partner;
import id.payu.partner.domain.PaymentLink;
import id.payu.partner.dto.CreatePaymentLinkRequest;
import id.payu.partner.dto.PaymentLinkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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

    private PaymentLinkService paymentLinkService;

    private Partner activePartner;

    @BeforeEach
    void setUp() {
        paymentLinkService = new PaymentLinkService(paymentLinkRepository, partnerRepository);

        activePartner = new Partner("Test Partner", "MERCHANT", "test@partner.com", "08123456789", "api-key-1");
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
            when(paymentLinkRepository.save(any(PaymentLink.class)))
                    .thenAnswer(invocation -> {
                        PaymentLink saved = invocation.getArgument(0);
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

            ArgumentCaptor<PaymentLink> captor = ArgumentCaptor.forClass(PaymentLink.class);
            verify(paymentLinkRepository).save(captor.capture());
            PaymentLink saved = captor.getValue();
            assertEquals(PaymentLink.PaymentLinkStatus.ACTIVE, saved.getStatus());
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
            Partner inactivePartner = new Partner("Inactive", "MERCHANT", "x@y.com", "08111", "key");
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
            PaymentLink link = new PaymentLink(activePartner, BigDecimal.valueOf(100000), "IDR",
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
            PaymentLink link = new PaymentLink(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().minusHours(1));
            link.setId(1L);

            when(paymentLinkRepository.findBySlug("expired123")).thenReturn(Optional.of(link));
            when(paymentLinkRepository.save(any(PaymentLink.class))).thenAnswer(i -> i.getArgument(0));

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
            PaymentLink link = new PaymentLink(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().plusHours(24));
            link.setId(1L);

            when(paymentLinkRepository.findBySlug("pay123")).thenReturn(Optional.of(link));
            when(paymentLinkRepository.save(any(PaymentLink.class))).thenAnswer(i -> i.getArgument(0));

            PaymentLinkResponse response = paymentLinkService.confirmPayment("pay123", "WALLET", "ref-001");

            assertEquals("PAID", response.getStatus());
            assertEquals("WALLET", response.getPaymentMethod());
            assertEquals("ref-001", response.getPaymentReference());
            assertNotNull(response.getPaidAt());
        }

        @Test
        @DisplayName("should fail for expired payment link")
        void shouldFailForExpiredPaymentLink() {
            PaymentLink link = new PaymentLink(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().minusHours(1));
            link.setId(1L);

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
            PaymentLink link = new PaymentLink(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Test payment", LocalDateTime.now().plusHours(24));
            link.setId(10L);

            when(paymentLinkRepository.findById(10L)).thenReturn(Optional.of(link));

            assertDoesNotThrow(() -> paymentLinkService.cancelPaymentLink(1L, 10L));

            verify(paymentLinkRepository).save(any(PaymentLink.class));
        }

        @Test
        @DisplayName("should fail cancel for wrong partner")
        void shouldFailCancelForWrongPartner() {
            Partner otherPartner = new Partner("Other", "MERCHANT", "o@y.com", "08111", "key");
            otherPartner.setId(99L);

            PaymentLink link = new PaymentLink(otherPartner, BigDecimal.valueOf(100000), "IDR",
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
            PaymentLink link1 = new PaymentLink(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Link 1", LocalDateTime.now().plusHours(24));
            link1.setId(1L);

            PageRequest pageable = PageRequest.of(0, 10);
            Page<PaymentLink> page = new PageImpl<>(List.of(link1), pageable, 1);

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
        @DisplayName("should expire past-due active links")
        void shouldExpirePastDueActiveLinks() {
            PaymentLink link = new PaymentLink(activePartner, BigDecimal.valueOf(100000), "IDR",
                    "Expired", LocalDateTime.now().minusHours(1));
            link.setId(1L);

            when(paymentLinkRepository.findExpiredActiveLinks(any(LocalDateTime.class)))
                    .thenReturn(List.of(link));
            when(paymentLinkRepository.saveAll(anyList())).thenReturn(List.of(link));

            paymentLinkService.expirePaymentLinks();

            assertEquals(PaymentLink.PaymentLinkStatus.EXPIRED, link.getStatus());
            verify(paymentLinkRepository).saveAll(anyList());
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
