package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.UniversalSearchResponse;
import id.payu.backoffice.adapter.persistence.repository.CustomerCaseRepository;
import id.payu.backoffice.adapter.persistence.repository.FraudCaseRepository;
import id.payu.backoffice.adapter.persistence.repository.KycReviewRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Service tests require Docker + PostgreSQL environment")
@Transactional
class UniversalSearchServiceTest {

    @Autowired
    UniversalSearchService searchService;

    @Autowired
    KycReviewRepository kycReviewRepository;

    @Autowired
    FraudCaseRepository fraudCaseRepository;

    @Autowired
    CustomerCaseRepository customerCaseRepository;

    @Test
    @Transactional
    void testSearchByUserId() {
        String uniqueUserId = "testSearchByUserId_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.setUserId(uniqueUserId);
        kycReview.setAccountNumber("ACC_" + uniqueUserId);
        kycReview.setDocumentType("KTP");
        kycReview.setDocumentNumber("3201234567890001");
        kycReview.setFullName("John Doe");
        kycReview.setAddress("Jalan Test No. 123");
        kycReview.setPhoneNumber("08123456789");
        kycReview.setStatus(KycReview.KycStatus.PENDING);
        kycReview.setCreatedAt(LocalDateTime.now());
        kycReviewRepository.save(kycReview);

        FraudCase fraudCase = new FraudCase();
        fraudCase.setUserId(uniqueUserId);
        fraudCase.setAccountNumber("ACC_" + uniqueUserId);
        fraudCase.setTransactionId(UUID.randomUUID());
        fraudCase.setTransactionType("TRANSFER");
        fraudCase.setAmount(new BigDecimal("1000000"));
        fraudCase.setFraudType("Unauthorized Transaction");
        fraudCase.setRiskLevel(FraudCase.RiskLevel.HIGH);
        fraudCase.setStatus(FraudCase.CaseStatus.OPEN);
        fraudCase.setDescription("Unauthorized transfer detected");
        fraudCase.setCreatedAt(LocalDateTime.now());
        fraudCaseRepository.save(fraudCase);

        UniversalSearchResponse response = searchService.search(uniqueUserId, null, 0, 20);

        assertNotNull(response);
        assertEquals(uniqueUserId, response.query());
        assertTrue(response.totalResults() >= 2);

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.id().equals(kycReview.getId()) && r.type().equals("kyc"));
        boolean hasFraudCase = response.results().stream()
                .anyMatch(r -> r.id().equals(fraudCase.getId()) && r.type().equals("fraud"));

        assertTrue(hasKycReview, "Should find KYC review");
        assertTrue(hasFraudCase, "Should find fraud case");
    }

    @Test
    @Transactional
    void testSearchByAccountNumber() {
        String uniqueAccount = "ACC_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.setUserId("user_" + System.currentTimeMillis());
        kycReview.setAccountNumber(uniqueAccount);
        kycReview.setDocumentType("KTP");
        kycReview.setDocumentNumber("3201234567890002");
        kycReview.setFullName("Jane Doe");
        kycReview.setAddress("Jalan Test No. 456");
        kycReview.setPhoneNumber("08198765432");
        kycReview.setStatus(KycReview.KycStatus.PENDING);
        kycReview.setCreatedAt(LocalDateTime.now());
        kycReviewRepository.save(kycReview);

        FraudCase fraudCase = new FraudCase();
        fraudCase.setUserId("user_" + System.currentTimeMillis());
        fraudCase.setAccountNumber(uniqueAccount);
        fraudCase.setTransactionId(UUID.randomUUID());
        fraudCase.setTransactionType("TRANSFER");
        fraudCase.setAmount(new BigDecimal("2000000"));
        fraudCase.setFraudType("Suspicious Activity");
        fraudCase.setRiskLevel(FraudCase.RiskLevel.MEDIUM);
        fraudCase.setStatus(FraudCase.CaseStatus.OPEN);
        fraudCase.setDescription("Suspicious transaction pattern");
        fraudCase.setCreatedAt(LocalDateTime.now());
        fraudCaseRepository.save(fraudCase);

        UniversalSearchResponse response = searchService.search(uniqueAccount, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 2);

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.id().equals(kycReview.getId()) && r.type().equals("kyc"));
        boolean hasFraudCase = response.results().stream()
                .anyMatch(r -> r.id().equals(fraudCase.getId()) && r.type().equals("fraud"));

        assertTrue(hasKycReview, "Should find KYC review by account number");
        assertTrue(hasFraudCase, "Should find fraud case by account number");
    }

    @Test
    @Transactional
    void testSearchByDocumentNumber() {
        String uniqueDocumentNumber = "320" + System.currentTimeMillis() + "0001";
        String uniqueUserId = "user_doc_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.setUserId(uniqueUserId);
        kycReview.setAccountNumber("ACC_DOC_" + System.currentTimeMillis());
        kycReview.setDocumentType("KTP");
        kycReview.setDocumentNumber(uniqueDocumentNumber);
        kycReview.setFullName("Test User");
        kycReview.setAddress("Test Address");
        kycReview.setPhoneNumber("08111111111");
        kycReview.setStatus(KycReview.KycStatus.PENDING);
        kycReview.setCreatedAt(LocalDateTime.now());
        kycReviewRepository.save(kycReview);

        UniversalSearchResponse response = searchService.search(uniqueDocumentNumber, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.id().equals(kycReview.getId()) && r.type().equals("kyc"));
        assertTrue(hasKycReview, "Should find KYC review by document number");
    }

    @Test
    @Transactional
    void testSearchByFullName() {
        String uniqueName = "Fullname_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.setUserId("user_name_" + System.currentTimeMillis());
        kycReview.setAccountNumber("ACC_NAME_" + System.currentTimeMillis());
        kycReview.setDocumentType("KTP");
        kycReview.setDocumentNumber("3201234567890003");
        kycReview.setFullName(uniqueName);
        kycReview.setAddress("Test Address");
        kycReview.setPhoneNumber("08122222222");
        kycReview.setStatus(KycReview.KycStatus.PENDING);
        kycReview.setCreatedAt(LocalDateTime.now());
        kycReviewRepository.save(kycReview);

        UniversalSearchResponse response = searchService.search(uniqueName, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.id().equals(kycReview.getId()) && r.type().equals("kyc"));
        assertTrue(hasKycReview, "Should find KYC review by full name");
    }

    @Test
    @Transactional
    void testSearchByFraudType() {
        String uniqueFraudType = "FraudType_" + System.currentTimeMillis();

        FraudCase fraudCase = new FraudCase();
        fraudCase.setUserId("user_fraud_" + System.currentTimeMillis());
        fraudCase.setAccountNumber("ACC_FRAUD_" + System.currentTimeMillis());
        fraudCase.setTransactionId(UUID.randomUUID());
        fraudCase.setTransactionType("TRANSFER");
        fraudCase.setAmount(new BigDecimal("3000000"));
        fraudCase.setFraudType(uniqueFraudType);
        fraudCase.setRiskLevel(FraudCase.RiskLevel.HIGH);
        fraudCase.setStatus(FraudCase.CaseStatus.OPEN);
        fraudCase.setDescription("Test fraud case");
        fraudCase.setCreatedAt(LocalDateTime.now());
        fraudCaseRepository.save(fraudCase);

        UniversalSearchResponse response = searchService.search(uniqueFraudType, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasFraudCase = response.results().stream()
                .anyMatch(r -> r.id().equals(fraudCase.getId()) && r.type().equals("fraud"));
        assertTrue(hasFraudCase, "Should find fraud case by fraud type");
    }

    @Test
    @Transactional
    void testSearchByCaseNumber() {
        String uniqueCaseNumber = "CASE_" + System.currentTimeMillis();

        CustomerCase customerCase = new CustomerCase();
        customerCase.setUserId("user_case_" + System.currentTimeMillis());
        customerCase.setAccountNumber("ACC_CASE_" + System.currentTimeMillis());
        customerCase.setCaseNumber(uniqueCaseNumber);
        customerCase.setCaseType(CustomerCase.CaseType.TRANSACTION_DISPUTE);
        customerCase.setPriority(CustomerCase.Priority.HIGH);
        customerCase.setSubject("Test case");
        customerCase.setDescription("Test case description");
        customerCase.setStatus(CustomerCase.CaseStatus.OPEN);
        customerCase.setCreatedAt(LocalDateTime.now());
        customerCaseRepository.save(customerCase);

        UniversalSearchResponse response = searchService.search(uniqueCaseNumber, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasCustomerCase = response.results().stream()
                .anyMatch(r -> r.id().equals(customerCase.getId()) && r.type().equals("customer"));
        assertTrue(hasCustomerCase, "Should find customer case by case number");
    }

    @Test
    @Transactional
    void testSearchBySubject() {
        String uniqueSubject = "Subject_" + System.currentTimeMillis();

        CustomerCase customerCase = new CustomerCase();
        customerCase.setUserId("user_subject_" + System.currentTimeMillis());
        customerCase.setAccountNumber("ACC_SUBJECT_" + System.currentTimeMillis());
        customerCase.setCaseNumber("CASE_SUB_" + System.currentTimeMillis());
        customerCase.setCaseType(CustomerCase.CaseType.TRANSACTION_DISPUTE);
        customerCase.setPriority(CustomerCase.Priority.HIGH);
        customerCase.setSubject(uniqueSubject);
        customerCase.setDescription("Test subject case");
        customerCase.setStatus(CustomerCase.CaseStatus.OPEN);
        customerCase.setCreatedAt(LocalDateTime.now());
        customerCaseRepository.save(customerCase);

        UniversalSearchResponse response = searchService.search(uniqueSubject, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasCustomerCase = response.results().stream()
                .anyMatch(r -> r.id().equals(customerCase.getId()) && r.type().equals("customer"));
        assertTrue(hasCustomerCase, "Should find customer case by subject");
    }

    @Test
    @Transactional
    void testSearchByEntityType() {
        String uniqueUserId = "user_entity_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.setUserId(uniqueUserId);
        kycReview.setAccountNumber("ACC_ENT_" + System.currentTimeMillis());
        kycReview.setDocumentType("KTP");
        kycReview.setDocumentNumber("3201234567890004");
        kycReview.setFullName("Entity Test User");
        kycReview.setAddress("Entity Test Address");
        kycReview.setPhoneNumber("08133333333");
        kycReview.setStatus(KycReview.KycStatus.PENDING);
        kycReview.setCreatedAt(LocalDateTime.now());
        kycReviewRepository.save(kycReview);

        UniversalSearchResponse response = searchService.search(uniqueUserId, "kyc", 0, 20);

        assertNotNull(response);
        assertEquals(uniqueUserId, response.query());

        boolean hasOnlyKyc = response.results().stream()
                .allMatch(r -> r.type().equals("kyc"));
        assertTrue(hasOnlyKyc, "Should only return KYC reviews when entityType is 'kyc'");
    }

    @Test
    void testSearchWithPagination() {
        String uniqueQuery = "paginate_" + System.currentTimeMillis();

        UniversalSearchResponse page1 = searchService.search(uniqueQuery, null, 0, 1);
        UniversalSearchResponse page2 = searchService.search(uniqueQuery, null, 1, 1);

        assertNotNull(page1);
        assertNotNull(page2);
        assertEquals(0, page1.page());
        assertEquals(1, page2.page());
        assertTrue(page1.results().size() <= 1);
        assertTrue(page2.results().size() <= 1);
    }

    @Test
    void testSearchNoResults() {
        String nonExistentQuery = "nonexistent_" + System.currentTimeMillis() + "_" + UUID.randomUUID();
        UniversalSearchResponse response = searchService.search(nonExistentQuery, null, 0, 20);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
        assertTrue(response.results().isEmpty());
    }

    @Test
    void testSearchEmptyQuery() {
        UniversalSearchResponse response = searchService.search("", null, 0, 20);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
    }

    @Test
    @Transactional
    void testSearchResultItemStructure() {
        String uniqueUserId = "user_struct_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.setUserId(uniqueUserId);
        kycReview.setAccountNumber("ACC_STRUCT_" + System.currentTimeMillis());
        kycReview.setDocumentType("KTP");
        kycReview.setDocumentNumber("3201234567890005");
        kycReview.setFullName("Structure Test User");
        kycReview.setAddress("Structure Test Address");
        kycReview.setPhoneNumber("08144444444");
        kycReview.setStatus(KycReview.KycStatus.PENDING);
        kycReview.setCreatedAt(LocalDateTime.now());
        kycReviewRepository.save(kycReview);

        UniversalSearchResponse response = searchService.search(uniqueUserId, null, 0, 20);

        UniversalSearchResponse.SearchResultItem kycItem = response.results().stream()
                .filter(r -> r.id().equals(kycReview.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(kycItem);
        assertEquals("kyc", kycItem.type());
        assertEquals(kycReview.getId(), kycItem.id());
        assertNotNull(kycItem.title());
        assertNotNull(kycItem.description());
        assertEquals(uniqueUserId, kycItem.userId());
        assertNotNull(kycItem.accountNumber());
        assertEquals("PENDING", kycItem.status());
        assertNotNull(kycItem.createdAt());
        assertNotNull(kycItem.details());
    }
}
