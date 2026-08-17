package id.payu.backoffice.interfaces.dto;

import static org.junit.jupiter.api.Assertions.*;

import id.payu.backoffice.domain.KycReview;
import org.junit.jupiter.api.Test;

class KycPiiMaskingTest {
    @Test
    void responseNeverExposesFullPiiOrDocumentUrl() {
        KycReview review = KycReview.create("user", "1234567890", "KTP", "3201234567890001",
                "https://private/document", "Jane Doe", "Private Address", "+628123456789", "Sensitive note");
        KycReviewResponse response = KycReviewResponse.from(review);

        assertEquals("****7890", response.accountNumber());
        assertEquals("****user", response.userId());
        assertEquals("****0001", response.documentNumber());
        assertEquals("J****", response.fullName());
        assertEquals("****", response.address());
        assertEquals("****6789", response.phoneNumber());
        assertNull(response.documentUrl());
        assertEquals("[REDACTED]", response.notes());
        String serialized = response.toString();
        for (String pii : new String[]{"1234567890", "3201234567890001", "Jane Doe",
                "Private Address", "+628123456789", "https://private/document", "Sensitive note"}) {
            assertFalse(serialized.contains(pii), pii);
        }
    }
}
