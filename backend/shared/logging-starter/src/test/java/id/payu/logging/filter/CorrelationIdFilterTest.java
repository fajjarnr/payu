package id.payu.logging.filter;

import id.payu.logging.config.PayuLoggingProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private PayuLoggingProperties properties;
    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        properties = new PayuLoggingProperties();
        filter = new CorrelationIdFilter(properties);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Correlation ID generation")
    class CorrelationIdGeneration {

        @Test
        @DisplayName("should generate new correlation ID when header is missing")
        void shouldGenerateNewIdWhenHeaderMissing() throws Exception {
            filter.doFilter(request, response, (req, res) -> {
                String id = MDC.get("correlation_id");
                assertThat(id).isNotNull().hasSize(32); // UUID without dashes
            });
        }

        @Test
        @DisplayName("should reuse correlation ID from incoming header")
        void shouldReuseIdFromHeader() throws Exception {
            String existingId = "abc123def456";
            request.addHeader("X-Correlation-Id", existingId);

            filter.doFilter(request, response, (req, res) -> {
                assertThat(MDC.get("correlation_id")).isEqualTo(existingId);
            });
        }

        @Test
        @DisplayName("should generate new ID when header is blank")
        void shouldGenerateIdWhenHeaderBlank() throws Exception {
            request.addHeader("X-Correlation-Id", "   ");

            filter.doFilter(request, response, (req, res) -> {
                String id = MDC.get("correlation_id");
                assertThat(id).isNotNull().isNotBlank().hasSize(32);
            });
        }
    }

    @Nested
    @DisplayName("MDC lifecycle")
    class MdcLifecycle {

        @Test
        @DisplayName("should set service metadata in MDC during request")
        void shouldSetServiceMetadata() throws Exception {
            properties.setServiceName("test-service");
            properties.setServiceVersion("2.0.0");
            properties.setEnvironment("staging");

            filter.doFilter(request, response, (req, res) -> {
                assertThat(MDC.get("service")).isEqualTo("test-service");
                assertThat(MDC.get("service_version")).isEqualTo("2.0.0");
                assertThat(MDC.get("environment")).isEqualTo("staging");
            });
        }

        @Test
        @DisplayName("should clean up MDC after request completes")
        void shouldCleanUpMdcAfterRequest() throws Exception {
            filter.doFilter(request, response, chain);

            assertThat(MDC.get("correlation_id")).isNull();
            assertThat(MDC.get("service")).isNull();
            assertThat(MDC.get("service_version")).isNull();
            assertThat(MDC.get("environment")).isNull();
        }

        @Test
        @DisplayName("should clean up MDC even when chain throws exception")
        void shouldCleanUpOnException() throws Exception {
            try {
                filter.doFilter(request, response, (req, res) -> {
                    throw new RuntimeException("boom");
                });
            } catch (RuntimeException ignored) {
            }

            assertThat(MDC.get("correlation_id")).isNull();
            assertThat(MDC.get("service")).isNull();
        }
    }

    @Nested
    @DisplayName("Response header")
    class ResponseHeader {

        @Test
        @DisplayName("should set correlation ID in response header")
        void shouldSetResponseHeader() throws Exception {
            request.addHeader("X-Correlation-Id", "resp-test-id");

            filter.doFilter(request, response, chain);

            assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("resp-test-id");
        }

        @Test
        @DisplayName("should set generated correlation ID in response header")
        void shouldSetGeneratedIdInResponseHeader() throws Exception {
            filter.doFilter(request, response, chain);

            assertThat(response.getHeader("X-Correlation-Id")).isNotNull().hasSize(32);
        }
    }

    @Nested
    @DisplayName("Custom configuration")
    class CustomConfiguration {

        @Test
        @DisplayName("should use custom header name and MDC key")
        void shouldUseCustomHeaderAndMdcKey() throws Exception {
            properties.getCorrelation().setHeaderName("X-Request-Id");
            properties.getCorrelation().setMdcKey("request_id");
            filter = new CorrelationIdFilter(properties);

            request.addHeader("X-Request-Id", "custom-123");

            filter.doFilter(request, response, (req, res) -> {
                assertThat(MDC.get("request_id")).isEqualTo("custom-123");
                assertThat(MDC.get("correlation_id")).isNull();
            });

            assertThat(response.getHeader("X-Request-Id")).isEqualTo("custom-123");
        }
    }
}
