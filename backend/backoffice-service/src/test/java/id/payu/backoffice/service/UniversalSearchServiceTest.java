package id.payu.backoffice.service;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.UniversalSearchResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UniversalSearchServiceTest {

    @Inject
    UniversalSearchService universalSearchService;

    @Test
    @Transactional
    void testSearchByUserId() {
        String uniqueUserId = "testSearchByUserId_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.userId = uniqueUserId;
        kycReview.accountNumber = "ACC_" + uniqueUserId;
        kycReview.documentType = "KTP";
        kycReview.documentNumber = "3201234567890001";
        kycReview.fullName = "John Doe";
        kycReview.address = "Jalan Test No. 123";
        kycReview.phoneNumber = "08123456789";
        kycReview.status = KycReview.KycStatus.PENDING;
        kycReview.createdAt = LocalDateTime.now();
        kycReview.persist();

        FraudCase fraudCase = new FraudCase();
        fraudCase.userId = uniqueUserId;
        fraudCase.accountNumber = "ACC_" + uniqueUserId;
        fraudCase.transactionId = UUID.randomUUID();
        fraudCase.transactionType = "TRANSFER";
        fraudCase.amount = new BigDecimal("1000000");
        fraudCase.fraudType = "Unauthorized Transaction";
        fraudCase.riskLevel = FraudCase.RiskLevel.HIGH;
        fraudCase.status = FraudCase.CaseStatus.OPEN;
        fraudCase.description = "Unauthorized transfer detected";
        fraudCase.createdAt = LocalDateTime.now();
        fraudCase.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueUserId, null, 0, 20);

        assertNotNull(response);
        assertEquals(uniqueUserId, response.query());
        assertTrue(response.totalResults() >= 2);

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.id().equals(kycReview.id) && r.type().equals("kyc"));
        boolean hasFraudCase = response.results().stream()
                .anyMatch(r -> r.id().equals(fraudCase.id) && r.type().equals("fraud"));

        assertTrue(hasKycReview, "Should find KYC review");
        assertTrue(hasFraudCase, "Should find fraud case");
    }

    @Test
    @Transactional
    void testSearchByAccountNumber() {
        String uniqueAccount = "ACC_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.userId = "user_" + System.currentTimeMillis();
        kycReview.accountNumber = uniqueAccount;
        kycReview.documentType = "KTP";
        kycReview.documentNumber = "3201234567890002";
        kycReview.fullName = "Jane Doe";
        kycReview.address = "Jalan Test No. 456";
        kycReview.phoneNumber = "08198765432";
        kycReview.status = KycReview.KycStatus.PENDING;
        kycReview.createdAt = LocalDateTime.now();
        kycReview.persist();

        FraudCase fraudCase = new FraudCase();
        fraudCase.userId = "user_" + System.currentTimeMillis();
        fraudCase.accountNumber = uniqueAccount;
        fraudCase.transactionId = UUID.randomUUID();
        fraudCase.transactionType = "TRANSFER";
        fraudCase.amount = new BigDecimal("2000000");
        fraudCase.fraudType = "Suspicious Activity";
        fraudCase.riskLevel = FraudCase.RiskLevel.MEDIUM;
        fraudCase.status = FraudCase.CaseStatus.OPEN;
        fraudCase.description = "Suspicious transaction pattern";
        fraudCase.createdAt = LocalDateTime.now();
        fraudCase.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueAccount, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 2);

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.id().equals(kycReview.id) && r.type().equals("kyc"));
        boolean hasFraudCase = response.results().stream()
                .anyMatch(r -> r.id().equals(fraudCase.id) && r.type().equals("fraud"));

        assertTrue(hasKycReview, "Should find KYC review by account number");
        assertTrue(hasFraudCase, "Should find fraud case by account number");
    }

    @Test
    @Transactional
    void testSearchByDocumentNumber() {
        String uniqueDocumentNumber = "320" + System.currentTimeMillis() + "0001";
        String uniqueUserId = "user_doc_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.userId = uniqueUserId;
        kycReview.accountNumber = "ACC_DOC_" + System.currentTimeMillis();
        kycReview.documentType = "KTP";
        kycReview.documentNumber = uniqueDocumentNumber;
        kycReview.fullName = "Test User";
        kycReview.address = "Test Address";
        kycReview.phoneNumber = "08111111111";
        kycReview.status = KycReview.KycStatus.PENDING;
        kycReview.createdAt = LocalDateTime.now();
        kycReview.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueDocumentNumber, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.id().equals(kycReview.id) && r.type().equals("kyc"));
        assertTrue(hasKycReview, "Should find KYC review by document number");
    }

    @Test
    @Transactional
    void testSearchByFullName() {
        String uniqueName = "Fullname_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.userId = "user_name_" + System.currentTimeMillis();
        kycReview.accountNumber = "ACC_NAME_" + System.currentTimeMillis();
        kycReview.documentType = "KTP";
        kycReview.documentNumber = "3201234567890003";
        kycReview.fullName = uniqueName;
        kycReview.address = "Test Address";
        kycReview.phoneNumber = "08122222222";
        kycReview.status = KycReview.KycStatus.PENDING;
        kycReview.createdAt = LocalDateTime.now();
        kycReview.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueName, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasKycReview = response.results().stream()
                .anyMatch(r -> r.id().equals(kycReview.id) && r.type().equals("kyc"));
        assertTrue(hasKycReview, "Should find KYC review by full name");
    }

    @Test
    @Transactional
    void testSearchByFraudType() {
        String uniqueFraudType = "FraudType_" + System.currentTimeMillis();

        FraudCase fraudCase = new FraudCase();
        fraudCase.userId = "user_fraud_" + System.currentTimeMillis();
        fraudCase.accountNumber = "ACC_FRAUD_" + System.currentTimeMillis();
        fraudCase.transactionId = UUID.randomUUID();
        fraudCase.transactionType = "TRANSFER";
        fraudCase.amount = new BigDecimal("3000000");
        fraudCase.fraudType = uniqueFraudType;
        fraudCase.riskLevel = FraudCase.RiskLevel.HIGH;
        fraudCase.status = FraudCase.CaseStatus.OPEN;
        fraudCase.description = "Test fraud case";
        fraudCase.createdAt = LocalDateTime.now();
        fraudCase.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueFraudType, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasFraudCase = response.results().stream()
                .anyMatch(r -> r.id().equals(fraudCase.id) && r.type().equals("fraud"));
        assertTrue(hasFraudCase, "Should find fraud case by fraud type");
    }

    @Test
    @Transactional
    void testSearchByCaseNumber() {
        String uniqueCaseNumber = "CASE_" + System.currentTimeMillis();

        CustomerCase customerCase = new CustomerCase();
        customerCase.userId = "user_case_" + System.currentTimeMillis();
        customerCase.accountNumber = "ACC_CASE_" + System.currentTimeMillis();
        customerCase.caseNumber = uniqueCaseNumber;
        customerCase.caseType = CustomerCase.CaseType.TRANSACTION_DISPUTE;
        customerCase.priority = CustomerCase.Priority.HIGH;
        customerCase.subject = "Test case";
        customerCase.description = "Test case description";
        customerCase.status = CustomerCase.CaseStatus.OPEN;
        customerCase.createdAt = LocalDateTime.now();
        customerCase.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueCaseNumber, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasCustomerCase = response.results().stream()
                .anyMatch(r -> r.id().equals(customerCase.id) && r.type().equals("customer"));
        assertTrue(hasCustomerCase, "Should find customer case by case number");
    }

    @Test
    @Transactional
    void testSearchBySubject() {
        String uniqueSubject = "Subject_" + System.currentTimeMillis();

        CustomerCase customerCase = new CustomerCase();
        customerCase.userId = "user_subject_" + System.currentTimeMillis();
        customerCase.accountNumber = "ACC_SUBJECT_" + System.currentTimeMillis();
        customerCase.caseNumber = "CASE_SUB_" + System.currentTimeMillis();
        customerCase.caseType = CustomerCase.CaseType.TRANSACTION_DISPUTE;
        customerCase.priority = CustomerCase.Priority.HIGH;
        customerCase.subject = uniqueSubject;
        customerCase.description = "Test subject case";
        customerCase.status = CustomerCase.CaseStatus.OPEN;
        customerCase.createdAt = LocalDateTime.now();
        customerCase.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueSubject, null, 0, 20);

        assertNotNull(response);
        assertTrue(response.totalResults() >= 1);

        boolean hasCustomerCase = response.results().stream()
                .anyMatch(r -> r.id().equals(customerCase.id) && r.type().equals("customer"));
        assertTrue(hasCustomerCase, "Should find customer case by subject");
    }

    @Test
    @Transactional
    void testSearchByEntityType() {
        String uniqueUserId = "user_entity_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.userId = uniqueUserId;
        kycReview.accountNumber = "ACC_ENT_" + System.currentTimeMillis();
        kycReview.documentType = "KTP";
        kycReview.documentNumber = "3201234567890004";
        kycReview.fullName = "Entity Test User";
        kycReview.address = "Entity Test Address";
        kycReview.phoneNumber = "08133333333";
        kycReview.status = KycReview.KycStatus.PENDING;
        kycReview.createdAt = LocalDateTime.now();
        kycReview.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueUserId, "kyc", 0, 20);

        assertNotNull(response);
        assertEquals(uniqueUserId, response.query());

        boolean hasOnlyKyc = response.results().stream()
                .allMatch(r -> r.type().equals("kyc"));
        assertTrue(hasOnlyKyc, "Should only return KYC reviews when entityType is 'kyc'");
    }

    @Test
    void testSearchWithPagination() {
        String uniqueQuery = "paginate_" + System.currentTimeMillis();

        UniversalSearchResponse page1 = universalSearchService.search(uniqueQuery, null, 0, 1);
        UniversalSearchResponse page2 = universalSearchService.search(uniqueQuery, null, 1, 1);

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
        UniversalSearchResponse response = universalSearchService.search(nonExistentQuery, null, 0, 20);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
        assertTrue(response.results().isEmpty());
    }

    @Test
    void testSearchEmptyQuery() {
        UniversalSearchResponse response = universalSearchService.search("", null, 0, 20);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
    }

    @Test
    @Transactional
    void testSearchResultItemStructure() {
        String uniqueUserId = "user_struct_" + System.currentTimeMillis();

        KycReview kycReview = new KycReview();
        kycReview.userId = uniqueUserId;
        kycReview.accountNumber = "ACC_STRUCT_" + System.currentTimeMillis();
        kycReview.documentType = "KTP";
        kycReview.documentNumber = "3201234567890005";
        kycReview.fullName = "Structure Test User";
        kycReview.address = "Structure Test Address";
        kycReview.phoneNumber = "08144444444";
        kycReview.status = KycReview.KycStatus.PENDING;
        kycReview.createdAt = LocalDateTime.now();
        kycReview.persist();

        UniversalSearchResponse response = universalSearchService.search(uniqueUserId, null, 0, 20);

        UniversalSearchResponse.SearchResultItem kycItem = response.results().stream()
                .filter(r -> r.id().equals(kycReview.id))
                .findFirst()
                .orElse(null);

        assertNotNull(kycItem);
        assertEquals("kyc", kycItem.type());
        assertEquals(kycReview.id, kycItem.id());
        assertNotNull(kycItem.title());
        assertNotNull(kycItem.description());
        assertEquals(uniqueUserId, kycItem.userId());
        assertNotNull(kycItem.accountNumber());
        assertEquals("PENDING", kycItem.status());
        assertNotNull(kycItem.createdAt());
        assertNotNull(kycItem.details());
    }
}