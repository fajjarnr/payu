package id.payu.backoffice.service;

import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.KycReviewDecisionRequest;
import id.payu.backoffice.dto.KycReviewRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class KycReviewServiceTest {

    @Inject
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
        assertNotNull(result.id);
        assertEquals(testUserId, result.userId);
        assertEquals("ACC-001", result.accountNumber);
        assertEquals("PASSPORT", result.documentType);
        assertEquals("A1234567", result.documentNumber);
        assertEquals("http://example.com/doc.jpg", result.documentUrl);
        assertEquals("John Doe", result.fullName);
        assertEquals("123 Main St, Jakarta", result.address);
        assertEquals("+628123456789", result.phoneNumber);
        assertEquals("Initial KYC submission", result.notes);
        assertEquals(KycReview.KycStatus.PENDING, result.status);
        assertNotNull(result.createdAt);
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
        assertEquals(testUserId, result.userId);
        assertEquals("ACC-002", result.accountNumber);
        assertEquals("Minimal submission", result.notes);
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

        Optional<KycReview> result = kycReviewService.getById(review.id);

        assertTrue(result.isPresent());
        assertEquals(review.id, result.get().id);
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
        assertEquals(testUserId, result.get().userId);
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

        List<KycReview> results = kycReviewService.listByStatus(KycReview.KycStatus.PENDING, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(r -> r.status == KycReview.KycStatus.PENDING));
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
                KycReviewDecisionRequest.KycReviewStatus.APPROVED,
                "Documents verified, KYC approved"
        );

        KycReview result = kycReviewService.review(review.id, decisionRequest, "admin1");

        assertNotNull(result);
        assertEquals(KycReview.KycStatus.APPROVED, result.status);
        assertEquals("Documents verified, KYC approved", result.notes);
        assertEquals("admin1", result.reviewedBy);
        assertNotNull(result.reviewedAt);
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
                KycReviewDecisionRequest.KycReviewStatus.REJECTED,
                "Document blurry, please resubmit"
        );

        KycReview result = kycReviewService.review(review.id, decisionRequest, "admin2");

        assertEquals(KycReview.KycStatus.REJECTED, result.status);
        assertEquals("Document blurry, please resubmit", result.notes);
        assertEquals("admin2", result.reviewedBy);
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
                KycReviewDecisionRequest.KycReviewStatus.REQUIRES_ADDITIONAL_INFO,
                "Please provide proof of address"
        );

        KycReview result = kycReviewService.review(review.id, decisionRequest, "admin3");

        assertEquals(KycReview.KycStatus.REQUIRES_ADDITIONAL_INFO, result.status);
        assertEquals("Please provide proof of address", result.notes);
    }

    @Test
    @Transactional
    void testReview_NotFound() {
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
                KycReviewDecisionRequest.KycReviewStatus.APPROVED,
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
        UUID reviewId = review.id;

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
        assertEquals(uniqueUserId, result.get().userId);
        assertEquals("ACC-002", result.get().accountNumber);
    }
}
