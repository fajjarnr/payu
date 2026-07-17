package id.payu.backoffice.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReviewStateInvariantTest {
    @Test
    void terminalKycCannotBeReviewedAgainAndActorIsRequired() {
        KycReview review = KycReview.create("user", "account", "KTP", "doc", null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> review.review(KycStatus.APPROVED, null, " "));
        review.review(KycStatus.APPROVED, null, "reviewer");
        assertThrows(IllegalStateException.class, () -> review.review(KycStatus.REJECTED, null, "reviewer"));
    }

    @Test
    void fraudResolutionRequiresActorAndTerminalCaseCannotTransition() {
        FraudCase fraud = FraudCase.create("user", "account", null, "TRANSFER", BigDecimal.ONE,
                "TYPE", RiskLevel.HIGH, "description", null);
        assertThrows(IllegalArgumentException.class,
                () -> fraud.resolve(FraudCaseStatus.RESOLVED, null, null));
        fraud.resolve(FraudCaseStatus.RESOLVED, null, "reviewer");
        assertThrows(IllegalStateException.class,
                () -> fraud.resolve(FraudCaseStatus.UNDER_INVESTIGATION, null, "reviewer"));
    }
}
