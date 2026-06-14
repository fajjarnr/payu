package id.payu.backoffice.application.service;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity;
import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import id.payu.backoffice.domain.CaseType;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.FraudCaseStatus;
import id.payu.backoffice.domain.KycStatus;
import id.payu.backoffice.domain.Priority;
import id.payu.backoffice.domain.RiskLevel;
import id.payu.backoffice.dto.UniversalSearchResponse;
import id.payu.backoffice.adapter.persistence.repository.CustomerCaseRepository;
import id.payu.backoffice.adapter.persistence.repository.FraudCaseRepository;
import id.payu.backoffice.adapter.persistence.repository.KycReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UniversalSearchService using Mockito mocks.
 * Converted from @SpringBootTest + @Disabled to pure unit tests (BUG-TEST-052).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UniversalSearchService Unit Tests")
class UniversalSearchServiceTest {

    @Mock
    private KycReviewRepository kycReviewRepository;

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private CustomerCaseRepository customerCaseRepository;

    @InjectMocks
    private UniversalSearchService searchService;

    private KycReviewEntity sampleKycReview;
    private FraudCaseEntity sampleFraudCase;
    private CustomerCaseEntity sampleCustomerCase;

    @BeforeEach
    void setUp() {
        sampleKycReview = new KycReviewEntity();
        sampleKycReview.setId(UUID.randomUUID());
        sampleKycReview.setUserId("testUser123");
        sampleKycReview.setAccountNumber("ACC_TEST_123");
        sampleKycReview.setDocumentType("KTP");
        sampleKycReview.setDocumentNumber("3201234567890001");
        sampleKycReview.setFullName("John Doe");
        sampleKycReview.setAddress("Jalan Test No. 123");
        sampleKycReview.setPhoneNumber("08123456789");
        sampleKycReview.setStatus(KycStatus.PENDING);
        sampleKycReview.setCreatedAt(LocalDateTime.now());

        sampleFraudCase = new FraudCaseEntity();
        sampleFraudCase.setUserId("testUser123");
        sampleFraudCase.setAccountNumber("ACC_TEST_123");
        sampleFraudCase.setTransactionId(UUID.randomUUID());
        sampleFraudCase.setTransactionType("TRANSFER");
        sampleFraudCase.setAmount(new BigDecimal("1000000"));
        sampleFraudCase.setFraudType("Unauthorized Transaction");
        sampleFraudCase.setRiskLevel(RiskLevel.HIGH);
        sampleFraudCase.setStatus(FraudCaseStatus.OPEN);
        sampleFraudCase.setDescription("Unauthorized transfer detected");
        sampleFraudCase.setCreatedAt(LocalDateTime.now());

        sampleCustomerCase = new CustomerCaseEntity();
        sampleCustomerCase.setUserId("testUser456");
        sampleCustomerCase.setAccountNumber("ACC_TEST_456");
        sampleCustomerCase.setCaseNumber("CASE_TEST_001");
        sampleCustomerCase.setCaseType(CaseType.TRANSACTION_DISPUTE);
        sampleCustomerCase.setPriority(Priority.HIGH);
        sampleCustomerCase.setSubject("Test case");
        sampleCustomerCase.setDescription("Test case description");
        sampleCustomerCase.setStatus(CustomerCaseStatus.OPEN);
        sampleCustomerCase.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should find results by userId across KYC and Fraud repositories")
    void testSearchByUserId() {
        String query = "testUser123";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of(sampleKycReview));
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of(sampleFraudCase));
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        assertNotNull(response);
        assertEquals(query, response.query());
        assertEquals(2, response.totalResults());

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.type().equals("kyc"));
        boolean hasFraudCase = response.results().stream()
                .anyMatch(r -> r.type().equals("fraud"));

        assertTrue(hasKycReview, "Should find KYC review");
        assertTrue(hasFraudCase, "Should find fraud case");
    }

    @Test
    @DisplayName("Should find results by account number")
    void testSearchByAccountNumber() {
        String query = "ACC_TEST_123";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of(sampleKycReview));
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of(sampleFraudCase));
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 2);

        boolean hasKycReview = response.results().stream().anyMatch(r -> r.type().equals("kyc"));
        boolean hasFraudCase = response.results().stream().anyMatch(r -> r.type().equals("fraud"));

        assertTrue(hasKycReview, "Should find KYC review by account number");
        assertTrue(hasFraudCase, "Should find fraud case by account number");
    }

    @Test
    @DisplayName("Should find results by document number")
    void testSearchByDocumentNumber() {
        String query = "3201234567890001";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of(sampleKycReview));
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasKycReview = response.results().stream().anyMatch(r -> r.type().equals("kyc"));
        assertTrue(hasKycReview, "Should find KYC review by document number");
    }

    @Test
    @DisplayName("Should find results by full name")
    void testSearchByFullName() {
        String query = "John Doe";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of(sampleKycReview));

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasKycReview = response.results().stream().anyMatch(r -> r.type().equals("kyc"));
        assertTrue(hasKycReview, "Should find KYC review by full name");
    }

    @Test
    @DisplayName("Should find results by fraud type")
    void testSearchByFraudType() {
        String query = "Unauthorized Transaction";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of(sampleFraudCase));

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasFraudCase = response.results().stream().anyMatch(r -> r.type().equals("fraud"));
        assertTrue(hasFraudCase, "Should find fraud case by fraud type");
    }

    @Test
    @DisplayName("Should find results by case number")
    void testSearchByCaseNumber() {
        String query = "CASE_TEST_001";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of(sampleCustomerCase));
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasCustomerCase = response.results().stream().anyMatch(r -> r.type().equals("customer"));
        assertTrue(hasCustomerCase, "Should find customer case by case number");
    }

    @Test
    @DisplayName("Should find results by subject")
    void testSearchBySubject() {
        String query = "Test case";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of(sampleCustomerCase));

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasCustomerCase = response.results().stream().anyMatch(r -> r.type().equals("customer"));
        assertTrue(hasCustomerCase, "Should find customer case by subject");
    }

    @Test
    @DisplayName("Should filter by entity type when specified")
    void testSearchByEntityType() {
        String query = "testUser123";
        // When entityType is "kyc", only KYC repositories should be queried
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of(sampleKycReview));
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, "kyc", 0, 20);

        assertNotNull(response);
        assertEquals(query, response.query());

        boolean hasOnlyKyc = response.results().stream().allMatch(r -> r.type().equals("kyc"));
        assertTrue(hasOnlyKyc, "Should only return KYC reviews when entityType is 'kyc'");

        // Fraud and customer repositories should NOT have been called
        verifyNoInteractions(fraudCaseRepository);
        verifyNoInteractions(customerCaseRepository);
    }

    @Test
    @DisplayName("Should paginate results correctly")
    void testSearchWithPagination() {
        String query = "paginate_test";
        // Return empty lists for all repositories
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse page1 = searchService.search(query, null, 0, 1);
        UniversalSearchResponse page2 = searchService.search(query, null, 1, 1);

        assertNotNull(page1);
        assertNotNull(page2);
        assertEquals(0, page1.page());
        assertEquals(1, page2.page());
        assertTrue(page1.results().size() <= 1);
        assertTrue(page2.results().size() <= 1);
    }

    @Test
    @DisplayName("Should return empty results for non-matching query")
    void testSearchNoResults() {
        String query = "nonexistent_query";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
        assertTrue(response.results().isEmpty());
    }

    @Test
    @DisplayName("Should return empty results for empty query")
    void testSearchEmptyQuery() {
        UniversalSearchResponse response = searchService.search("", null, 0, 20);

        assertNotNull(response);
        assertEquals(0, response.totalResults());

        // No repository interactions for empty query
        verifyNoInteractions(kycReviewRepository);
        verifyNoInteractions(fraudCaseRepository);
        verifyNoInteractions(customerCaseRepository);
    }

    @Test
    @DisplayName("Should return proper result item structure")
    void testSearchResultItemStructure() {
        String query = "testUser123";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of(sampleKycReview));
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        UniversalSearchResponse.SearchResultItem kycItem = response.results().stream()
                .filter(r -> r.type().equals("kyc"))
                .findFirst()
                .orElse(null);

        assertNotNull(kycItem);
        assertEquals("kyc", kycItem.type());
        assertEquals(sampleKycReview.getId(), kycItem.id());
        assertNotNull(kycItem.title());
        assertNotNull(kycItem.description());
        assertEquals("testUser123", kycItem.userId());
        assertNotNull(kycItem.accountNumber());
        assertEquals("PENDING", kycItem.status());
        assertNotNull(kycItem.createdAt());
        assertNotNull(kycItem.details());
    }

    @Test
    @DisplayName("Should deduplicate results found by multiple fields")
    void testSearchDeduplication() {
        // Same KYC review found by both userId and accountNumber
        String query = "testUser123";
        when(kycReviewRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of(sampleKycReview));
        when(kycReviewRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of(sampleKycReview));
        when(kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(kycReviewRepository.findByFullNameContainingIgnoreCase(query)).thenReturn(List.of());

        when(fraudCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query)).thenReturn(List.of());

        when(customerCaseRepository.findByUserIdContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByAccountNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findByCaseNumberContainingIgnoreCase(query)).thenReturn(List.of());
        when(customerCaseRepository.findBySubjectContainingIgnoreCase(query)).thenReturn(List.of());

        UniversalSearchResponse response = searchService.search(query, null, 0, 20);

        // Should only contain one KYC review, not duplicated
        long kycCount = response.results().stream().filter(r -> r.type().equals("kyc")).count();
        assertEquals(1, kycCount, "Duplicate KYC reviews should be deduplicated");
    }
}
