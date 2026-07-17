package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.domain.KycStatus;
import id.payu.backoffice.dto.KycReviewDecisionRequest;
import id.payu.backoffice.dto.KycReviewRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KycReviewServiceTest {

    @Autowired
    KycReviewService kycReviewService;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-" + System.currentTimeMillis();
    }

    // Create KYC Review Tests

    @Test
    @Transactional
    void testCreateKycReview_Success() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-001",
                "PASSPORT",
                "A1234567",
                "http://example.com/doc.jpg",
                "John Doe",
                "123 Main St, Jakarta",
                "+628123456789",
                "Initial KYC submission"
        );

        KycReview result = kycReviewService.create(request);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals("ACC-001", result.getAccountNumber());
        assertEquals("PASSPORT", result.getDocumentType());
        assertEquals("A1234567", result.getDocumentNumber());
        assertEquals("http://example.com/doc.jpg", result.getDocumentUrl());
        assertEquals("John Doe", result.getFullName());
        assertEquals("123 Main St, Jakarta", result.getAddress());
        assertEquals("+628123456789", result.getPhoneNumber());
        assertEquals("Initial KYC submission", result.getNotes());
        assertEquals(KycStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    @Transactional
    void testCreateKycReview_MinimalFields() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-002",
                null,
                null,
                null,
                null,
                null,
                null,
                "Minimal submission"
        );

        KycReview result = kycReviewService.create(request);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals("ACC-002", result.getAccountNumber());
        assertEquals("Minimal submission", result.getNotes());
    }

    // Query KYC Review Tests

    @Test
    @Transactional
    void testGetById_Success() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-QUERY",
                "KTP",
                "3201234567890001",
                "http://example.com/ktp.jpg",
                "Jane Doe",
                "456 Oak Ave, Surabaya",
                "+628987654321",
                "Test review"
        );

        KycReview review = kycReviewService.create(request);

        Optional<KycReview> result = kycReviewService.getById(review.getId());

        assertTrue(result.isPresent());
        assertEquals(review.getId(), result.get().getId());
    }

    @Test
    void testGetById_NotFound() {
        Optional<KycReview> result = kycReviewService.getById(UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testGetByUserId_Success() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-USER",
                "KTP",
                "3201234567890001",
                "http://example.com/ktp.jpg",
                "Jane Doe",
                "456 Oak Ave, Surabaya",
                "+628987654321",
                "Test review"
        );

        kycReviewService.create(request);

        Optional<KycReview> result = kycReviewService.getByUserId(testUserId);

        assertTrue(result.isPresent());
        assertEquals(testUserId, result.get().getUserId());
    }

    @Test
    void testGetByUserId_NotFound() {
        Optional<KycReview> result = kycReviewService.getByUserId("nonexistent-user");

        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testListByStatus_Success() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-STATUS",
                "KTP",
                "3201234567890001",
                "http://example.com/ktp.jpg",
                "Jane Doe",
                "456 Oak Ave, Surabaya",
                "+628987654321",
                "Test review"
        );

        kycReviewService.create(request);

        List<KycReview> results = kycReviewService.listByStatus(KycStatus.PENDING, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(r -> r.getStatus() == KycStatus.PENDING));
    }

    @Test
    @Transactional
    void testListAll_Success() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-ALL",
                "KTP",
                "3201234567890001",
                "http://example.com/ktp.jpg",
                "Jane Doe",
                "456 Oak Ave, Surabaya",
                "+628987654321",
                "Test review"
        );

        kycReviewService.create(request);

        List<KycReview> results = kycReviewService.listAll(0, 10);

        assertNotNull(results);
        assertTrue(results.size() >= 1);
    }

    // Review KYC Tests

    @Test
    @Transactional
    void testReview_AsApproved() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-REVIEW-1",
                "KTP",
                "3201234567890002",
                "http://example.com/ktp2.jpg",
                "Bob Smith",
                "789 Pine Rd, Bandung",
                "+628555555555",
                "Review test"
        );

        KycReview review = kycReviewService.create(request);

        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
                id.payu.backoffice.dto.KycReviewStatus.APPROVED,
                "Documents verified, KYC approved"
        );

        KycReview result = kycReviewService.review(review.getId(), decisionRequest, "admin1");

        assertNotNull(result);
        assertEquals(KycStatus.APPROVED, result.getStatus());
        assertEquals("Documents verified, KYC approved", result.getNotes());
        assertEquals("admin1", result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    @Transactional
    void testReview_AsRejected() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-REVIEW-2",
                "KTP",
                "3201234567890002",
                "http://example.com/ktp2.jpg",
                "Bob Smith",
                "789 Pine Rd, Bandung",
                "+628555555555",
                "Review test"
        );

        KycReview review = kycReviewService.create(request);

        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
                id.payu.backoffice.dto.KycReviewStatus.REJECTED,
                "Document blurry, please resubmit"
        );

        KycReview result = kycReviewService.review(review.getId(), decisionRequest, "admin2");

        assertEquals(KycStatus.REJECTED, result.getStatus());
        assertEquals("Document blurry, please resubmit", result.getNotes());
        assertEquals("admin2", result.getReviewedBy());
    }

    @Test
    @Transactional
    void testReview_AsRequiresAdditionalInfo() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-REVIEW-3",
                "KTP",
                "3201234567890002",
                "http://example.com/ktp2.jpg",
                "Bob Smith",
                "789 Pine Rd, Bandung",
                "+628555555555",
                "Review test"
        );

        KycReview review = kycReviewService.create(request);

        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
                id.payu.backoffice.dto.KycReviewStatus.REQUIRES_ADDITIONAL_INFO,
                "Please provide proof of address"
        );

        KycReview result = kycReviewService.review(review.getId(), decisionRequest, "admin3");

        assertEquals(KycStatus.REQUIRES_ADDITIONAL_INFO, result.getStatus());
        assertEquals("Please provide proof of address", result.getNotes());
    }

    @Test
    @Transactional
    void testReview_NotFound() {
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
                id.payu.backoffice.dto.KycReviewStatus.APPROVED,
                "Test"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            kycReviewService.review(UUID.randomUUID(), decisionRequest, "admin1");
        });
    }

    // Delete KYC Review Tests

    @Test
    @Transactional
    void testDelete_Success() {
        KycReviewRequest request = new KycReviewRequest(
                testUserId,
                "ACC-DELETE",
                "SIM",
                "1234567890123456",
                "http://example.com/sim.jpg",
                "Test User",
                "Test Address",
                "+628111111111",
                "Delete test"
        );

        KycReview review = kycReviewService.create(request);
        UUID reviewId = review.getId();

        kycReviewService.delete(reviewId);

        Optional<KycReview> result = kycReviewService.getById(reviewId);
        assertFalse(result.isPresent());
    }

    // GetByUserId Returns Latest Tests

    @Test
    @Transactional
    void testGetByUserId_ReturnsMostRecent() {
        String uniqueUserId = "user-latest-" + System.currentTimeMillis();

        // Create first review
        kycReviewService.create(new KycReviewRequest(
                uniqueUserId,
                "ACC-001",
                "KTP",
                "111",
                null,
                "User One",
                null,
                null,
                "First review"
        ));

        // Small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Create second review
        KycReviewRequest secondRequest = new KycReviewRequest(
                uniqueUserId,
                "ACC-002",
                "KTP",
                "222",
                null,
                "User One",
                null,
                null,
                "Second review"
        );
        kycReviewService.create(secondRequest);

        // Should return the most recent review
        Optional<KycReview> result = kycReviewService.getByUserId(uniqueUserId);

        assertTrue(result.isPresent());
        assertEquals(uniqueUserId, result.get().getUserId());
        assertEquals("ACC-002", result.get().getAccountNumber());
    }
}
