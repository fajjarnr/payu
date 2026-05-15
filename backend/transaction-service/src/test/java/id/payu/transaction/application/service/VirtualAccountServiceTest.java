package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.repository.VirtualAccountRepository;
import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;
import id.payu.transaction.dto.CreateVirtualAccountRequest;
import id.payu.transaction.dto.VaCallbackRequest;
import id.payu.transaction.dto.VirtualAccountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VirtualAccountServiceTest {

    @Mock
    private VirtualAccountRepository virtualAccountRepository;

    @InjectMocks
    private VirtualAccountService virtualAccountService;

    @Nested
    @DisplayName("createVirtualAccount")
    class CreateVaTests {

        @Test
        @DisplayName("should create BCA VA successfully")
        void shouldCreateBcaVaSuccessfully() {
            CreateVirtualAccountRequest request = CreateVirtualAccountRequest.builder()
                    .bankCode("BCA")
                    .partnerId(UUID.randomUUID())
                    .amount(new BigDecimal("500000"))
                    .description("Order #123")
                    .customerName("John Doe")
                    .expiryHours(24)
                    .build();

            when(virtualAccountRepository.existsByVaNumber(anyString())).thenReturn(false);
            when(virtualAccountRepository.save(any(VirtualAccountEntity.class))).thenAnswer(i -> {
                VirtualAccountEntity va = i.getArgument(0);
                va.setId(UUID.randomUUID());
                va.setCreatedAt(Instant.now());
                return va;
            });

            VirtualAccountResponse response = virtualAccountService.createVirtualAccount(request);

            assertNotNull(response);
            assertEquals("BCA", response.getBankCode());
            assertTrue(response.getVaNumber().startsWith("1234"));
            assertEquals(new BigDecimal("500000"), response.getAmount());
            assertEquals("PENDING", response.getStatus());
            assertEquals("John Doe", response.getCustomerName());
        }

        @Test
        @DisplayName("should create BNI VA successfully")
        void shouldCreateBniVaSuccessfully() {
            CreateVirtualAccountRequest request = CreateVirtualAccountRequest.builder()
                    .bankCode("BNI")
                    .partnerId(UUID.randomUUID())
                    .amount(new BigDecimal("250000"))
                    .build();

            when(virtualAccountRepository.existsByVaNumber(anyString())).thenReturn(false);
            when(virtualAccountRepository.save(any(VirtualAccountEntity.class))).thenAnswer(i -> {
                VirtualAccountEntity va = i.getArgument(0);
                va.setId(UUID.randomUUID());
                va.setCreatedAt(Instant.now());
                return va;
            });

            VirtualAccountResponse response = virtualAccountService.createVirtualAccount(request);

            assertNotNull(response);
            assertEquals("BNI", response.getBankCode());
            assertTrue(response.getVaNumber().startsWith("8800"));
        }

        @Test
        @DisplayName("should fail for invalid bank code")
        void shouldFailForInvalidBankCode() {
            CreateVirtualAccountRequest request = CreateVirtualAccountRequest.builder()
                    .bankCode("INVALID")
                    .partnerId(UUID.randomUUID())
                    .amount(BigDecimal.valueOf(100000))
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> virtualAccountService.createVirtualAccount(request));
        }
    }

    @Nested
    @DisplayName("handleBankCallback")
    class BankCallbackTests {

        @Test
        @DisplayName("should handle payment callback successfully")
        void shouldHandlePaymentCallback() {
            VirtualAccountEntity va = VirtualAccountEntity.builder()
                    .id(UUID.randomUUID())
                    .vaNumber("1234999888777666")
                    .bankCode("BCA")
                    .bankName("BCA")
                    .partnerId(UUID.randomUUID())
                    .amount(new BigDecimal("500000"))
                    .currency("IDR")
                    .status(VirtualAccountEntity.VaStatus.PENDING)
                    .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                    .createdAt(Instant.now())
                    .build();

            when(virtualAccountRepository.findByVaNumber("1234999888777666"))
                    .thenReturn(Optional.of(va));
            when(virtualAccountRepository.save(any(VirtualAccountEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            VaCallbackRequest callback = VaCallbackRequest.builder()
                    .vaNumber("1234999888777666")
                    .amount(new BigDecimal("500000"))
                    .paymentReference("BANK-REF-001")
                    .build();

            VirtualAccountResponse response = virtualAccountService.handleBankCallback(callback);

            assertEquals("PAID", response.getStatus());
            assertEquals(new BigDecimal("500000"), response.getPaidAmount());
            assertEquals("BANK-REF-001", response.getPaymentReference());
            assertNotNull(response.getPaidAt());
        }

        @Test
        @DisplayName("should fail for non-existent VA")
        void shouldFailForNonExistentVa() {
            when(virtualAccountRepository.findByVaNumber("9999"))
                    .thenReturn(Optional.empty());

            VaCallbackRequest callback = VaCallbackRequest.builder()
                    .vaNumber("9999")
                    .amount(BigDecimal.valueOf(100000))
                    .paymentReference("ref")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> virtualAccountService.handleBankCallback(callback));
        }

        @Test
        @DisplayName("should fail for expired VA")
        void shouldFailForExpiredVa() {
            VirtualAccountEntity va = VirtualAccountEntity.builder()
                    .id(UUID.randomUUID())
                    .vaNumber("1234expired")
                    .bankCode("BCA")
                    .status(VirtualAccountEntity.VaStatus.PENDING)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .createdAt(Instant.now().minus(25, ChronoUnit.HOURS))
                    .build();

            when(virtualAccountRepository.findByVaNumber("1234expired"))
                    .thenReturn(Optional.of(va));

            VaCallbackRequest callback = VaCallbackRequest.builder()
                    .vaNumber("1234expired")
                    .amount(BigDecimal.valueOf(100000))
                    .paymentReference("ref")
                    .build();

            assertThrows(IllegalStateException.class,
                    () -> virtualAccountService.handleBankCallback(callback));
        }
    }

    @Nested
    @DisplayName("expireVirtualAccounts")
    class ExpiryTests {

        @Test
        @DisplayName("should expire pending VAs past TTL")
        void shouldExpirePendingVAs() {
            VirtualAccountEntity va = VirtualAccountEntity.builder()
                    .id(UUID.randomUUID())
                    .vaNumber("1234old")
                    .bankCode("BCA")
                    .status(VirtualAccountEntity.VaStatus.PENDING)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .createdAt(Instant.now().minus(25, ChronoUnit.HOURS))
                    .build();

            when(virtualAccountRepository.findExpiredPendingVAs(any(Instant.class)))
                    .thenReturn(List.of(va));
            when(virtualAccountRepository.saveAll(anyList())).thenReturn(List.of(va));

            virtualAccountService.expireVirtualAccounts();

            assertEquals(VirtualAccountEntity.VaStatus.EXPIRED, va.getStatus());
            verify(virtualAccountRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("should do nothing when no expired VAs")
        void shouldDoNothingWhenNoExpiredVAs() {
            when(virtualAccountRepository.findExpiredPendingVAs(any(Instant.class)))
                    .thenReturn(List.of());

            virtualAccountService.expireVirtualAccounts();

            verify(virtualAccountRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("getById / getByVaNumber")
    class GetTests {

        @Test
        @DisplayName("should get VA by ID")
        void shouldGetVaById() {
            UUID vaId = UUID.randomUUID();
            VirtualAccountEntity va = VirtualAccountEntity.builder()
                    .id(vaId)
                    .vaNumber("1234111222333")
                    .bankCode("BCA")
                    .bankName("BCA")
                    .partnerId(UUID.randomUUID())
                    .amount(BigDecimal.valueOf(100000))
                    .currency("IDR")
                    .status(VirtualAccountEntity.VaStatus.PENDING)
                    .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                    .createdAt(Instant.now())
                    .build();

            when(virtualAccountRepository.findById(vaId)).thenReturn(Optional.of(va));

            VirtualAccountResponse response = virtualAccountService.getById(vaId);

            assertNotNull(response);
            assertEquals(vaId, response.getId());
            assertEquals("1234111222333", response.getVaNumber());
        }

        @Test
        @DisplayName("should fail for non-existent VA ID")
        void shouldFailForNonExistentId() {
            UUID vaId = UUID.randomUUID();
            when(virtualAccountRepository.findById(vaId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> virtualAccountService.getById(vaId));
        }
    }
}
