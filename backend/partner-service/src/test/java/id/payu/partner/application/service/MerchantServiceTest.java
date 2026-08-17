package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.MerchantQrPaymentRepository;
import id.payu.partner.adapter.persistence.repository.MerchantRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.entity.MerchantEntity;
import id.payu.partner.adapter.persistence.entity.MerchantQrPaymentEntity;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.domain.MerchantCategory;
import id.payu.partner.domain.MerchantStatus;
import id.payu.partner.domain.QrPaymentStatus;
import id.payu.partner.interfaces.dto.CreateMerchantRequest;
import id.payu.partner.interfaces.dto.CreateQrPaymentRequest;
import id.payu.partner.interfaces.dto.MerchantResponse;
import id.payu.partner.interfaces.dto.QrPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.outbox.service.OutboxService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantQrPaymentRepository qrPaymentRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private WebhookDispatcherService webhookDispatcher;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ObjectMapper objectMapper;

    private MerchantService merchantService;

    private PartnerEntity activePartner;
    private MerchantEntity activeMerchant;

    @BeforeEach
    void setUp() {
        merchantService = new MerchantService(merchantRepository, qrPaymentRepository, partnerRepository, webhookDispatcher, outboxService, objectMapper);

        activePartner = new PartnerEntity("Test PartnerEntity", "MERCHANT", "test@partner.com", "08123456789", "api-key-1");
        activePartner.setId(1L);
        activePartner.setActive(true);

        activeMerchant = new MerchantEntity(activePartner, "MCH001TEST", "Warung Kopi",
                MerchantCategory.FOOD_BEVERAGE, "Jl. Test 1");
        activeMerchant.setId(10L);
        activeMerchant.setStatus(MerchantStatus.ACTIVE);
    }

    @Nested
    @DisplayName("createMerchant")
    class CreateMerchantTests {

        @Test
        @DisplayName("should onboard merchant successfully")
        void shouldOnboardMerchantSuccessfully() {
            CreateMerchantRequest request = new CreateMerchantRequest();
            request.setBusinessName("Warung Kopi Nusantara");
            request.setCategory("FOOD_BEVERAGE");
            request.setAddress("Jl. Sudirman No. 1");
            request.setCity("Jakarta");
            request.setPicName("Budi");

            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(merchantRepository.existsByMerchantCode(anyString())).thenReturn(false);
            when(merchantRepository.save(any(MerchantEntity.class))).thenAnswer(invocation -> {
                MerchantEntity saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            MerchantResponse response = merchantService.createMerchant(1L, request);

            assertNotNull(response);
            assertEquals(100L, response.getId());
            assertEquals("Warung Kopi Nusantara", response.getBusinessName());
            assertEquals("FOOD_BEVERAGE", response.getCategory());
            assertEquals("PENDING_REVIEW", response.getStatus());
            assertNotNull(response.getMerchantCode());
            assertNotNull(response.getStaticQrCode());
        }

        @Test
        @DisplayName("should fail for inactive partner")
        void shouldFailForInactivePartner() {
            PartnerEntity inactive = new PartnerEntity("Inactive", "MERCHANT", "x@y.com", "08111", "key");
            inactive.setId(2L);
            inactive.setActive(false);
            when(partnerRepository.findById(2L)).thenReturn(Optional.of(inactive));

            CreateMerchantRequest request = new CreateMerchantRequest();
            request.setBusinessName("Test");
            request.setCategory("RETAIL");
            request.setAddress("Addr");

            assertThrows(IllegalStateException.class,
                    () -> merchantService.createMerchant(2L, request));
        }

        @Test
        @DisplayName("should fail for invalid category")
        void shouldFailForInvalidCategory() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));

            CreateMerchantRequest request = new CreateMerchantRequest();
            request.setBusinessName("Test");
            request.setCategory("INVALID_CATEGORY");
            request.setAddress("Addr");

            assertThrows(IllegalArgumentException.class,
                    () -> merchantService.createMerchant(1L, request));
        }
    }

    @Nested
    @DisplayName("activateMerchant")
    class ActivateMerchantTests {

        @Test
        @DisplayName("should activate pending merchant")
        void shouldActivatePendingMerchant() {
            MerchantEntity pending = new MerchantEntity(activePartner, "MCH002", "Test Store",
                    MerchantCategory.RETAIL, "Addr");
            pending.setId(20L);

            when(merchantRepository.findById(20L)).thenReturn(Optional.of(pending));
            when(merchantRepository.save(any(MerchantEntity.class))).thenAnswer(i -> i.getArgument(0));

            MerchantResponse response = merchantService.activateMerchant(20L);

            assertEquals("ACTIVE", response.getStatus());
        }

        @Test
        @DisplayName("should fail activating already active merchant")
        void shouldFailActivatingActiveMerchant() {
            when(merchantRepository.findById(10L)).thenReturn(Optional.of(activeMerchant));

            assertThrows(IllegalStateException.class,
                    () -> merchantService.activateMerchant(10L));
        }

        @Test
        @DisplayName("should reject merchant belonging to another partner (PARTNER-PROD-006)")
        void shouldRejectActivateMerchantOfAnotherPartner() {
            PartnerEntity otherPartner = new PartnerEntity("Other Corp", "MERCHANT", "other@x.com", "08122", "key");
            otherPartner.setId(2L);
            MerchantEntity otherMerchant = new MerchantEntity(otherPartner, "MCH099", "Other Store",
                    MerchantCategory.RETAIL, "Addr");
            otherMerchant.setId(99L);

            when(merchantRepository.findById(99L)).thenReturn(Optional.of(otherMerchant));

            assertThrows(IllegalArgumentException.class,
                    () -> merchantService.activateMerchantForPartner(1L, 99L));
            verify(merchantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getMerchantForPartner")
    class GetMerchantForPartnerTests {

        @Test
        @DisplayName("should return merchant owned by the partner")
        void shouldReturnOwnedMerchant() {
            MerchantEntity owned = new MerchantEntity(activePartner, "MCH002", "My Store",
                    MerchantCategory.RETAIL, "Addr");
            owned.setId(20L);

            when(merchantRepository.findById(20L)).thenReturn(Optional.of(owned));

            MerchantResponse response = merchantService.getMerchantForPartner(1L, 20L);

            assertEquals(20L, response.getId());
            assertEquals("My Store", response.getBusinessName());
        }

        @Test
        @DisplayName("should reject merchant belonging to another partner (PARTNER-PROD-006)")
        void shouldRejectMerchantOfAnotherPartner() {
            PartnerEntity otherPartner = new PartnerEntity("Other Corp", "MERCHANT", "other@x.com", "08122", "key");
            otherPartner.setId(2L);
            MerchantEntity otherMerchant = new MerchantEntity(otherPartner, "MCH099", "Other Store",
                    MerchantCategory.RETAIL, "Addr");
            otherMerchant.setId(99L);

            when(merchantRepository.findById(99L)).thenReturn(Optional.of(otherMerchant));

            assertThrows(IllegalArgumentException.class,
                    () -> merchantService.getMerchantForPartner(1L, 99L));
        }
    }

    @Nested
    @DisplayName("generateDynamicQr")
    class GenerateQrTests {

        @Test
        @DisplayName("should generate dynamic QR successfully")
        void shouldGenerateDynamicQr() {
            CreateQrPaymentRequest request = new CreateQrPaymentRequest();
            request.setAmount(new BigDecimal("50000.00"));
            request.setDescription("Kopi Susu 2x");
            request.setExpiryMinutes(30);

            when(merchantRepository.findById(10L)).thenReturn(Optional.of(activeMerchant));
            when(qrPaymentRepository.save(any(MerchantQrPaymentEntity.class))).thenAnswer(invocation -> {
                MerchantQrPaymentEntity saved = invocation.getArgument(0);
                saved.setId(200L);
                return saved;
            });

            QrPaymentResponse response = merchantService.generateDynamicQr(10L, request);

            assertNotNull(response);
            assertEquals(200L, response.getId());
            assertNotNull(response.getReferenceId());
            assertNotNull(response.getQrContent());
            assertTrue(response.getQrContent().contains("MCH001TEST"));
            assertEquals(new BigDecimal("50000.00"), response.getAmount());
            assertEquals("PENDING", response.getStatus());
            assertEquals("Warung Kopi", response.getMerchantName());
        }

        @Test
        @DisplayName("should fail for inactive merchant")
        void shouldFailForInactiveMerchant() {
            MerchantEntity suspended = new MerchantEntity(activePartner, "MCH003", "Suspended",
                    MerchantCategory.RETAIL, "Addr");
            suspended.setId(30L);
            suspended.setStatus(MerchantStatus.SUSPENDED);

            when(merchantRepository.findById(30L)).thenReturn(Optional.of(suspended));

            CreateQrPaymentRequest request = new CreateQrPaymentRequest();
            request.setAmount(BigDecimal.valueOf(10000));

            assertThrows(IllegalStateException.class,
                    () -> merchantService.generateDynamicQr(30L, request));
        }
    }

    @Nested
    @DisplayName("confirmQrPayment")
    class ConfirmQrPaymentTests {

        @Test
        @DisplayName("should confirm QR payment successfully")
        void shouldConfirmQrPayment() {
            MerchantQrPaymentEntity qrPayment = new MerchantQrPaymentEntity(
                    activeMerchant, BigDecimal.valueOf(50000), "IDR",
                    "Test", LocalDateTime.now().plusMinutes(30));
            qrPayment.setId(200L);

            when(qrPaymentRepository.markPaidIfPending(eq(qrPayment.getReferenceId()), eq("payer-acc-123"),
                    anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
            when(qrPaymentRepository.findByReferenceId(qrPayment.getReferenceId()))
                    .thenReturn(Optional.of(qrPayment), Optional.of(qrPayment));

            QrPaymentResponse response = merchantService.confirmQrPayment(
                    qrPayment.getReferenceId(), "payer-acc-123");

            assertEquals("PAID", response.getStatus());
            assertEquals("payer-acc-123", response.getPayerAccountId());
            assertNotNull(response.getPaymentReference());
            assertNotNull(response.getPaidAt());
            verify(qrPaymentRepository, never()).save(any(MerchantQrPaymentEntity.class));
        }

        @Test
        @DisplayName("double confirm on already-paid QR settles exactly once (IMP-2)")
        void shouldSettleExactlyOnceOnDoubleConfirm() {
            MerchantQrPaymentEntity qrPayment = new MerchantQrPaymentEntity(
                    activeMerchant, BigDecimal.valueOf(50000), "IDR",
                    "Test", LocalDateTime.now().plusMinutes(30));
            qrPayment.setId(201L);
            MerchantQrPaymentEntity paidQr = new MerchantQrPaymentEntity(
                    activeMerchant, BigDecimal.valueOf(50000), "IDR",
                    "Test", LocalDateTime.now().plusMinutes(30));
            paidQr.setId(201L);
            paidQr.markPaid("payer-acc-123", "QRIS-ABC123");

            when(qrPaymentRepository.markPaidIfPending(eq(qrPayment.getReferenceId()), eq("payer-acc-123"),
                    anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1, 0);
            when(qrPaymentRepository.findByReferenceId(qrPayment.getReferenceId()))
                    .thenReturn(Optional.of(qrPayment), Optional.of(paidQr));

            merchantService.confirmQrPayment(qrPayment.getReferenceId(), "payer-acc-123");
            QrPaymentResponse second = merchantService.confirmQrPayment(qrPayment.getReferenceId(), "payer-acc-123");

            assertEquals("PAID", second.getStatus());
            verify(webhookDispatcher, times(1)).dispatch(anyString(), anyMap());
        }

        @Test
        @DisplayName("should fail for expired QR payment")
        void shouldFailForExpiredQrPayment() {
            MerchantQrPaymentEntity qrPayment = new MerchantQrPaymentEntity(
                    activeMerchant, BigDecimal.valueOf(50000), "IDR",
                    "Test", LocalDateTime.now().minusMinutes(5));
            qrPayment.setId(201L);

            when(qrPaymentRepository.markPaidIfPending(eq(qrPayment.getReferenceId()), eq("payer-acc"),
                    anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0);
            when(qrPaymentRepository.findByReferenceId(qrPayment.getReferenceId()))
                    .thenReturn(Optional.of(qrPayment));

            assertThrows(IllegalStateException.class,
                    () -> merchantService.confirmQrPayment(qrPayment.getReferenceId(), "payer-acc"));
        }
    }

    @Nested
    @DisplayName("expireQrPayments")
    class ExpireTests {

        @Test
        @DisplayName("should expire pending QR payments via conditional transition (IMP-2)")
        void shouldExpirePendingPayments() {
            MerchantQrPaymentEntity qr = new MerchantQrPaymentEntity(
                    activeMerchant, BigDecimal.valueOf(10000), "IDR",
                    "Old", LocalDateTime.now().minusMinutes(5));
            qr.setId(300L);

            when(qrPaymentRepository.findExpiredPendingPayments(any(LocalDateTime.class)))
                    .thenReturn(List.of(qr));
            when(qrPaymentRepository.markExpiredIfPending(300L)).thenReturn(1);

            merchantService.expireQrPayments();

            verify(qrPaymentRepository).markExpiredIfPending(300L);
            verify(qrPaymentRepository, never()).saveAll(anyList());
        }
    }
}
