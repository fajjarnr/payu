package id.payu.backoffice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import id.payu.backoffice.domain.CaseType;
import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.FraudCaseStatus;
import id.payu.backoffice.domain.Priority;
import id.payu.backoffice.domain.RiskLevel;
import id.payu.backoffice.domain.port.outbound.CustomerCaseRepositoryPort;
import id.payu.backoffice.domain.port.outbound.FraudCaseRepositoryPort;
import id.payu.backoffice.domain.port.outbound.KycReviewRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UniversalSearchServiceTest {
    @Mock private KycReviewRepositoryPort kycRepository;
    @Mock private FraudCaseRepositoryPort fraudRepository;
    @Mock private CustomerCaseRepositoryPort customerRepository;
    @InjectMocks private UniversalSearchService service;

    private FraudCase fraudCase;
    private CustomerCase customerCase;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        fraudCase = FraudCase.reconstitute(
                UUID.randomUUID(), "testUser123", "ACC_TEST_123", UUID.randomUUID(), "TRANSFER",
                new BigDecimal("1000000"), "Unauthorized Transaction", RiskLevel.HIGH,
                FraudCaseStatus.OPEN, "Unauthorized transfer detected", null, null, null, null,
                null, now, null);
        customerCase = CustomerCase.reconstitute(
                UUID.randomUUID(), "testUser456", "ACC_TEST_456", "CASE_TEST_001",
                CaseType.TRANSACTION_DISPUTE, Priority.HIGH, "Test case", "Description",
                CustomerCaseStatus.OPEN, null, null, null, null, now, null);
    }

    @Test
    void doesNotRunSubstringSearchAgainstPii() {
        when(fraudRepository.findByFraudTypeContainingIgnoreCase("testUser123")).thenReturn(List.of());
        when(customerRepository.findByCaseNumberContainingIgnoreCase("testUser123")).thenReturn(List.of());
        when(customerRepository.findBySubjectContainingIgnoreCase("testUser123")).thenReturn(List.of());

        var response = service.search("testUser123", null, 0, 20);

        assertTrue(response.results().isEmpty());
        verifyNoInteractions(kycRepository);
        verify(fraudRepository, never()).findByUserIdContainingIgnoreCase("testUser123");
        verify(fraudRepository, never()).findByAccountNumberContainingIgnoreCase("testUser123");
        verify(customerRepository, never()).findByUserIdContainingIgnoreCase("testUser123");
        verify(customerRepository, never()).findByAccountNumberContainingIgnoreCase("testUser123");
    }

    @Test
    void searchesNonPiiFraudTypeAndMasksPii() {
        when(fraudRepository.findByFraudTypeContainingIgnoreCase("Unauthorized"))
                .thenReturn(List.of(fraudCase));

        var item = service.search("Unauthorized", "fraud", 0, 20).results().getFirst();

        assertEquals("fraud", item.type());
        assertEquals("****r123", item.userId());
        assertEquals("****_123", item.accountNumber());
        verifyNoInteractions(kycRepository, customerRepository);
    }

    @Test
    void searchesNonPiiCustomerFieldsDeduplicatesAndMasksPii() {
        when(customerRepository.findByCaseNumberContainingIgnoreCase("Test"))
                .thenReturn(List.of(customerCase));
        when(customerRepository.findBySubjectContainingIgnoreCase("Test"))
                .thenReturn(List.of(customerCase));

        var response = service.search("Test", "customer", 0, 20);

        assertEquals(1, response.totalResults());
        assertEquals("****r456", response.results().getFirst().userId());
        assertEquals("****_456", response.results().getFirst().accountNumber());
        verifyNoInteractions(kycRepository, fraudRepository);
    }

    @Test
    void filtersEntityTypeAndPaginates() {
        when(customerRepository.findByCaseNumberContainingIgnoreCase("Test"))
                .thenReturn(List.of(customerCase));
        when(customerRepository.findBySubjectContainingIgnoreCase("Test"))
                .thenReturn(List.of());

        var first = service.search("Test", "customer", 0, 1);
        var second = service.search("Test", "customer", 1, 1);

        assertEquals(1, first.results().size());
        assertTrue(second.results().isEmpty());
        verifyNoInteractions(kycRepository, fraudRepository);
    }

    @Test
    void emptyQueryDoesNotAccessRepositories() {
        var response = service.search("", null, 0, 20);

        assertEquals(0, response.totalResults());
        verifyNoInteractions(kycRepository, fraudRepository, customerRepository);
    }
}
