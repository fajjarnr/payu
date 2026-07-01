package id.payu.logging;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-21: Verify that {@code logback-payu-base.xml} wraps both console appenders
 * with {@code id.payu.security.masking.LogbackMaskingFilter} so PII is masked
 * before reaching LokiStack.
 *
 * <p>This test asserts wiring at the XML-resource level only (no classpath dep
 * on security-starter, avoiding transitive install cascade in unit tests).
 * The masking behavior of {@code LogbackMaskingFilter} itself is verified
 * inside the {@code security-starter} test suite.</p>
 */
class LogbackPiiMaskingIntegrationTest {

    private static final String MASKING_LAYOUT_FQN = "id.payu.security.masking.LogbackMaskingFilter";
    private static final String LAYOUT_WRAPPING_ENCODER_FQN =
        "ch.qos.logback.core.encoder.LayoutWrappingEncoder";

    private String loadLogbackBaseXml() throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("logback-payu-base.xml")) {
            assertThat(in)
                .as("logback-payu-base.xml must be on the classpath")
                .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void jsonConsoleAppenderShouldReferenceLogbackMaskingFilter() throws Exception {
        String xml = loadLogbackBaseXml();
        assertThat(extractAppenderBlock(xml, "JSON_CONSOLE"))
            .as("JSON_CONSOLE appender block must exist in logback-payu-base.xml")
            .contains(LAYOUT_WRAPPING_ENCODER_FQN)
            .contains(MASKING_LAYOUT_FQN);
    }

    @Test
    void textConsoleAppenderShouldReferenceLogbackMaskingFilter() throws Exception {
        String xml = loadLogbackBaseXml();
        assertThat(extractAppenderBlock(xml, "TEXT_CONSOLE"))
            .as("TEXT_CONSOLE appender block must exist in logback-payu-base.xml")
            .contains(LAYOUT_WRAPPING_ENCODER_FQN)
            .contains(MASKING_LAYOUT_FQN);
    }

    /**
     * Pulls the substring between {@code <appender name="X" ...>} and its matching
     * {@code </appender>} close tag (logback appender blocks have no nested appenders).
     */
    private String extractAppenderBlock(String xml, String appenderName) {
        String openTag = "<appender name=\"" + appenderName + "\"";
        int start = xml.indexOf(openTag);
        assertThat(start)
            .as("Appender <%s> must exist in logback-payu-base.xml", appenderName)
            .isGreaterThan(-1);
        int end = xml.indexOf("</appender>", start);
        assertThat(end)
            .as("Appender <%s> must have closing tag", appenderName)
            .isGreaterThan(start);
        return xml.substring(start, end);
    }
}
