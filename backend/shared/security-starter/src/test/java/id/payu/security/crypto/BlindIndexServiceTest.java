package id.payu.security.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BlindIndexService")
class BlindIndexServiceTest {

    private static final String KEY = "test-blind-index-key-at-least-32-chars!!";
    private static final String PREV_KEY = "previous-blind-index-key-at-least-32-chars";

    @Test
    @DisplayName("same value always yields the same index")
    void deterministicForSameValue() {
        BlindIndexService service = new BlindIndexService(KEY, "v1", "");
        assertThat(service.index("user@example.com")).isEqualTo(service.index("user@example.com"));
    }

    @Test
    @DisplayName("value case is preserved; callers normalize before indexing")
    void preservesCase() {
        BlindIndexService service = new BlindIndexService(KEY, "v1", "");
        assertThat(service.index("User@Example.com")).isNotEqualTo(service.index("user@example.com"));
    }

    @Test
    @DisplayName("rejects values with leading or trailing whitespace")
    void rejectsNonCanonicalWhitespace() {
        BlindIndexService service = new BlindIndexService(KEY, "v1", "");
        assertThatThrownBy(() -> service.index(" user@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.index("user@example.com "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null and blank values produce no index")
    void blankValueProducesNoIndex() {
        BlindIndexService service = new BlindIndexService(KEY, "v1", "");
        assertThat(service.index(null)).isNull();
        assertThat(service.index("   ")).isNull();
        assertThat(service.lookupIndexes("  ")).isEmpty();
    }

    @Test
    @DisplayName("lookupIndexes includes the previous key during rotation")
    void lookupCoversPreviousKeyDuringRotation() {
        BlindIndexService rotated = new BlindIndexService(KEY, "v2", "v1=" + PREV_KEY);
        BlindIndexService previous = new BlindIndexService(PREV_KEY, "v1", "");

        String oldIndex = previous.index("user@example.com");
        List<String> candidates = rotated.lookupIndexes("user@example.com");

        assertThat(candidates).contains(oldIndex);
        assertThat(rotated.index("user@example.com")).isNotEqualTo(oldIndex);
        assertThat(rotated.currentVersion()).isEqualTo("v2");
    }

    @Test
    @DisplayName("rejects short keys and malformed previous-key entries")
    void rejectsWeakConfiguration() {
        assertThatThrownBy(() -> new BlindIndexService("short", "v1", ""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new BlindIndexService(KEY, "v1", "broken"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new BlindIndexService(KEY, "", ""))
                .isInstanceOf(IllegalStateException.class);
    }
}
