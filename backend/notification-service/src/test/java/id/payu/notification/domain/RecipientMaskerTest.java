package id.payu.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PROD-045: masking rules for notification recipients in logs.
 * Emails, phone numbers and NIK-like values must never appear raw.
 */
class RecipientMaskerTest {

    @Test
    @DisplayName("masks email keeping domain")
    void masksEmailKeepingDomain() {
        assertThat(RecipientMasker.mask("user@example.com")).isEqualTo("u***@example.com");
    }

    @Test
    @DisplayName("masks short email fully")
    void masksShortEmailFully() {
        assertThat(RecipientMasker.mask("ab@x.io")).isEqualTo("***@x.io");
    }

    @Test
    @DisplayName("masks phone keeping last 4 digits")
    void masksPhoneKeepingLast4Digits() {
        assertThat(RecipientMasker.mask("+6281234567890")).isEqualTo("+628****7890");
    }

    @Test
    @DisplayName("masks NIK keeping last 4 digits")
    void masksNikKeepingLast4Digits() {
        assertThat(RecipientMasker.mask("3201012309000001")).isEqualTo("************0001");
    }

    @Test
    @DisplayName("handles null, blank and short values safely")
    void handlesNullBlankAndShortValuesSafely() {
        assertThat(RecipientMasker.mask(null)).isEqualTo("***");
        assertThat(RecipientMasker.mask("")).isEqualTo("***");
        assertThat(RecipientMasker.mask("ab")).isEqualTo("***");
    }
}
