package id.payu.wallet/config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization test for SecurityConfig — prevents regression of
 * <strong>E2E-2026-06-13-01</strong>.
 *
 * <p>Spring Security 6 / Spring Boot 3.5 PathPatternParser throws
 * PatternParseException: Multiple {*...} or ** pattern elements are not allowed
 * when the combined requestMatchers allowlist mixes 8 /** catch-alls with the
 * /api/v1/v1/public/** typo. Every protected /api/v1/* request then renders as
 * HTML 500 from the DispatcherServlet error dispatch.</p>
 *
 * <p>This test scans the production SecurityConfig.java source and asserts
 * two structural invariants that, if violated, re-introduce the bug:</p>
 * <ol>
 *   <li>The allowlist does not contain the /api/v1/v1/public/** typo.</li>
 *   <li>No single .requestMatchers(...) call line in the file carries
 *       more than 4 /** catch-all patterns (the parser refuses
 *       arbitrary combinations above that threshold).</li>
 * </ol>
 */
class SecurityConfigPatternTest {

    private static final Path SOURCE = Paths.get(
            "src/main/java/id/payu/wallet/config/SecurityConfig.java");

    @Test
    @DisplayName("SecurityConfig should not contain the /api/v1/v1/public/** typo")
    void noDoubleV1Typo() throws Exception {
        String content = Files.readString(SOURCE);
        assertThat(content)
                .as("the duplicate /api/v1/v1/public/** typo was the trigger for the PatternParseException")
                .doesNotContain("/api/v1/v1/public/**");
    }

    @Test
    @DisplayName("Each requestMatchers call must carry fewer than 5 /** catch-alls")
    void noRequestMatchersLineCarriesTooManyCatchAlls() throws Exception {
        List<String> requestMatcherLines = Files.readString(SOURCE)
                .lines()
                .filter(l -> l.contains(".requestMatchers(") && l.contains("/**"))
                .collect(Collectors.toList());

        assertThat(requestMatcherLines)
                .allSatisfy(line -> {
                    long catchalls = (line.length() - line.replace("/**", "").length()) / 2;
                    assertThat(catchalls)
                            .as("line carries %d /** patterns which triggers PatternParseException: %s",
                                    catchalls, line.trim())
                            .isLessThan(5L);
                });
    }
}
