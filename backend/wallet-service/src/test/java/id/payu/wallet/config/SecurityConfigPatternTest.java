package id.payu.wallet.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization test for the {@code PatternParseException: Multiple {*...} or
 * ** pattern elements are not allowed} bug (E2E-2026-06-13-01 follow-up for
 * wallet-service: the 1.8.11 fix missed wallet-service which had the same
 * 6-pattern requestMatchers call).
 *
 * <p>Bug: a single {@code .requestMatchers(...).permitAll()} call with
 * multiple {@code /**} patterns (e.g. {@code "/actuator/**",
 * "/swagger-ui/**"}) makes Spring 6 {@code PathPatternParser} throw
 * {@code PatternParseException} on the first request, which the
 * DispatcherServlet surfaces as an HTML 500. The fix: split the
 * {@code requestMatchers} call into one call per pattern.</p>
 */
@DisplayName("Security Config Pattern — no multiple /** in one requestMatchers call")
class SecurityConfigPatternTest {

    private static final String SECURITY_CONFIG_PATH =
        "src/main/java/id/payu/wallet/config/SecurityConfig.java";

    private static final Pattern MULTI_STAR =
        Pattern.compile("\\.requestMatchers\\(([^)]*\\*\\*[^)]*)\\)\\.permitAll");

    private static final Pattern TYPO_PATTERN =
        Pattern.compile("/api/v1/v1/public");

    @Test
    @DisplayName("SecurityConfig has no requestMatchers with multiple /** patterns")
    void noMultipleStarInSingleRequestMatchersCall() throws IOException {
        String src = Files.readString(Path.of(SECURITY_CONFIG_PATH));
        var matcher = MULTI_STAR.matcher(src);
        if (matcher.find()) {
            String captured = matcher.group(1);
            long starCount = Pattern.compile("\\*\\*").matcher(captured).results().count();
            assertEquals(1, starCount,
                "Found requestMatchers call with " + starCount + " /** patterns in one call. "
                    + "Split into one requestMatchers call per pattern (PathPatternParser "
                    + "disallows multiple /** in a single expression). Offending call: "
                    + "requestMatchers(" + captured + ").permitAll()");
        }
    }

    @Test
    @DisplayName("SecurityConfig has no /api/v1/v1/public/** typo (historical bug)")
    void noApiV1V1PublicTypo() throws IOException {
        String src = Files.readString(Path.of(SECURITY_CONFIG_PATH));
        assertTrue(!TYPO_PATTERN.matcher(src).find(),
            "Found /api/v1/v1/public/** typo (E2E-2026-06-13-01 historical bug)");
    }
}
