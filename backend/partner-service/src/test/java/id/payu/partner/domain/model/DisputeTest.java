package id.payu.partner.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain tests for Dispute aggregate root.
 * TDD: Red phase - tests first, implementation second.
 */
@DisplayName("Dispute Domain Model Tests")
class DisputeTest {

    @Nested
    @DisplayName("Dispute Creation")
    class DisputeCreationTests {

        @Test
        @DisplayName("should create dispute with open status")
        void shouldCreateDisputeWithOpenStatus() {
            UUID transactionId = UUID.randomUUID();
            String partnerId = "partner-123";
            String reason = "Unauthorized transaction";
            String openedBy = "customer@example.com";

            Dispute dispute = Dispute.open(transactionId, partnerId, reason, openedBy);

            assertNotNull(dispute.getId());
            assertEquals(transactionId, dispute.getTransactionId());
            assertEquals(partnerId, dispute.getPartnerId());
            assertEquals(reason, dispute.getReason());
            assertEquals(openedBy, dispute.getOpenedBy());
            assertEquals(DisputeStatus.OPEN, dispute.getStatus());
            assertNotNull(dispute.getOpenedAt());
            assertNull(dispute.getResolvedAt());
            assertNull(dispute.getResolution());
            assertTrue(dispute.getEvidenceUrls().isEmpty());
        }

        @Test
        @DisplayName("should not create dispute with null transaction id")
        void shouldNotCreateDisputeWithNullTransactionId() {
            assertThrows(IllegalArgumentException.class, () ->
                Dispute.open(null, "partner-123", "reason", "user")
            );
        }

        @Test
        @DisplayName("should not create dispute with blank partner id")
        void shouldNotCreateDisputeWithBlankPartnerId() {
            UUID transactionId = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () ->
                Dispute.open(transactionId, "", "reason", "user")
            );

            assertThrows(IllegalArgumentException.class, () ->
                Dispute.open(transactionId, "   ", "reason", "user")
            );
        }

        @Test
        @DisplayName("should not create dispute with blank reason")
        void shouldNotCreateDisputeWithBlankReason() {
            UUID transactionId = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () ->
                Dispute.open(transactionId, "partner-123", "", "user")
            );

            assertThrows(IllegalArgumentException.class, () ->
                Dispute.open(transactionId, "partner-123", "   ", "user")
            );
        }
    }

    @Nested
    @DisplayName("Investigation")
    class InvestigationTests {

        @Test
        @DisplayName("should start investigation from open status")
        void shouldStartInvestigation() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            String investigatorId = "investigator-456";

            dispute.startInvestigation(investigatorId);

            assertEquals(DisputeStatus.UNDER_INVESTIGATION, dispute.getStatus());
            assertEquals(investigatorId, dispute.getInvestigatorId());
            assertNotNull(dispute.getInvestigationStartedAt());
        }

        @Test
        @DisplayName("should not start investigation if not in open status")
        void shouldNotStartInvestigationIfNotOpen() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            dispute.startInvestigation("investigator-456");

            assertThrows(IllegalStateException.class, () ->
                dispute.startInvestigation("investigator-789")
            );
        }

        @Test
        @DisplayName("should not start investigation with blank investigator id")
        void shouldNotStartInvestigationWithBlankInvestigatorId() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );

            assertThrows(IllegalArgumentException.class, () ->
                dispute.startInvestigation("")
            );
        }
    }

    @Nested
    @DisplayName("Dispute Resolution")
    class DisputeResolutionTests {

        @Test
        @DisplayName("should resolve dispute from under investigation status")
        void shouldResolveDispute() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            dispute.startInvestigation("investigator-456");
            String resolution = "Customer verified transaction. Dispute rejected.";

            dispute.resolve(resolution);

            assertEquals(DisputeStatus.RESOLVED, dispute.getStatus());
            assertEquals(resolution, dispute.getResolution());
            assertNotNull(dispute.getResolvedAt());
        }

        @Test
        @DisplayName("should not resolve dispute without investigation")
        void shouldNotResolveDisputeWithoutInvestigation() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );

            assertThrows(IllegalStateException.class, () ->
                dispute.resolve("Some resolution")
            );
        }

        @Test
        @DisplayName("should not resolve already resolved dispute")
        void shouldNotResolveAlreadyResolvedDispute() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            dispute.startInvestigation("investigator-456");
            dispute.resolve("Resolution 1");

            assertThrows(IllegalStateException.class, () ->
                dispute.resolve("Resolution 2")
            );
        }

        @Test
        @DisplayName("should not resolve with blank resolution")
        void shouldNotResolveWithBlankResolution() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            dispute.startInvestigation("investigator-456");

            assertThrows(IllegalArgumentException.class, () ->
                dispute.resolve("")
            );
        }
    }

    @Nested
    @DisplayName("Dispute Rejection")
    class DisputeRejectionTests {

        @Test
        @DisplayName("should reject dispute from under investigation status")
        void shouldRejectDispute() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            dispute.startInvestigation("investigator-456");
            String reason = "Insufficient evidence provided";

            dispute.reject(reason);

            assertEquals(DisputeStatus.REJECTED, dispute.getStatus());
            assertEquals(reason, dispute.getRejectionReason());
            assertNotNull(dispute.getResolvedAt());
        }

        @Test
        @DisplayName("should not reject dispute without investigation")
        void shouldNotRejectDisputeWithoutInvestigation() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );

            assertThrows(IllegalStateException.class, () ->
                dispute.reject("Some reason")
            );
        }

        @Test
        @DisplayName("should not reject with blank reason")
        void shouldNotRejectWithBlankReason() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            dispute.startInvestigation("investigator-456");

            assertThrows(IllegalArgumentException.class, () ->
                dispute.reject("")
            );
        }
    }

    @Nested
    @DisplayName("Evidence Management")
    class EvidenceManagementTests {

        @Test
        @DisplayName("should add evidence to dispute")
        void shouldAddEvidence() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            String evidenceUrl = "https://storage.payu.com/evidence/123.pdf";

            dispute.addEvidence(evidenceUrl);

            List<String> evidence = dispute.getEvidenceUrls();
            assertEquals(1, evidence.size());
            assertEquals(evidenceUrl, evidence.get(0));
        }

        @Test
        @DisplayName("should add multiple evidence items")
        void shouldAddMultipleEvidenceItems() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );

            dispute.addEvidence("https://storage.payu.com/evidence/1.pdf");
            dispute.addEvidence("https://storage.payu.com/evidence/2.jpg");
            dispute.addEvidence("https://storage.payu.com/evidence/3.png");

            assertEquals(3, dispute.getEvidenceUrls().size());
        }

        @Test
        @DisplayName("should not add blank evidence url")
        void shouldNotAddBlankEvidenceUrl() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );

            assertThrows(IllegalArgumentException.class, () ->
                dispute.addEvidence("")
            );
        }

        @Test
        @DisplayName("should not add duplicate evidence url")
        void shouldNotAddDuplicateEvidenceUrl() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            String evidenceUrl = "https://storage.payu.com/evidence/123.pdf";

            dispute.addEvidence(evidenceUrl);

            assertThrows(IllegalArgumentException.class, () ->
                dispute.addEvidence(evidenceUrl)
            );
        }

        @Test
        @DisplayName("should not add evidence to resolved dispute")
        void shouldNotAddEvidenceToResolvedDispute() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(),
                "partner-123",
                "Unauthorized transaction",
                "customer@example.com"
            );
            dispute.startInvestigation("investigator-456");
            dispute.resolve("Resolved");

            assertThrows(IllegalStateException.class, () ->
                dispute.addEvidence("https://storage.payu.com/evidence/123.pdf")
            );
        }
    }

    @Nested
    @DisplayName("Dispute Status Checks")
    class DisputeStatusChecksTests {

        @Test
        @DisplayName("should correctly identify open status")
        void shouldCorrectlyIdentifyOpenStatus() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(), "partner-123",
                "reason", "user"
            );
            assertTrue(dispute.isOpen());
            assertFalse(dispute.isUnderInvestigation());
            assertFalse(dispute.isClosed());
        }

        @Test
        @DisplayName("should correctly identify under investigation status")
        void shouldCorrectlyIdentifyUnderInvestigationStatus() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(), "partner-123",
                "reason", "user"
            );
            dispute.startInvestigation("investigator-123");
            assertFalse(dispute.isOpen());
            assertTrue(dispute.isUnderInvestigation());
            assertFalse(dispute.isClosed());
        }

        @Test
        @DisplayName("should correctly identify closed status")
        void shouldCorrectlyIdentifyClosedStatus() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(), "partner-123",
                "reason", "user"
            );
            dispute.startInvestigation("investigator-123");
            dispute.resolve("Resolved");
            assertFalse(dispute.isOpen());
            assertFalse(dispute.isUnderInvestigation());
            assertTrue(dispute.isClosed());
        }

        @Test
        @DisplayName("should correctly identify rejected as closed")
        void shouldCorrectlyIdentifyRejectedAsClosed() {
            Dispute dispute = Dispute.open(
                UUID.randomUUID(), "partner-123",
                "reason", "user"
            );
            dispute.startInvestigation("investigator-123");
            dispute.reject("Rejected");
            assertTrue(dispute.isClosed());
        }
    }
}
