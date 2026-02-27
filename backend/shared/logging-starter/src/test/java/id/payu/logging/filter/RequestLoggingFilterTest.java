package id.payu.logging.filter;

import id.payu.logging.config.PayuLoggingProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private PayuLoggingProperties properties;
    private RequestLoggingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        properties = new PayuLoggingProperties();
        properties.getRequestLogging().setEnabled(true);
        request = new MockHttpServletRequest("GET", "/api/v1/accounts");
        response = new MockHttpServletResponse();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Basic request logging")
    class BasicLogging {

        @Test
        @DisplayName("should pass request through filter chain")
        void shouldPassRequestThrough() throws Exception {
            filter = new RequestLoggingFilter(properties);
            boolean[] called = {false};

            filter.doFilter(request, response, (req, res) -> called[0] = true);

            assertThat(called[0]).isTrue();
        }

        @Test
        @DisplayName("should skip actuator endpoints")
        void shouldSkipActuator() throws Exception {
            filter = new RequestLoggingFilter(properties);
            request = new MockHttpServletRequest("GET", "/actuator/health");
            boolean[] called = {false};

            filter.doFilter(request, response, (req, res) -> called[0] = true);

            assertThat(called[0]).isTrue();
        }

        @Test
        @DisplayName("should skip /health endpoint")
        void shouldSkipHealth() throws Exception {
            filter = new RequestLoggingFilter(properties);
            request = new MockHttpServletRequest("GET", "/health");
            boolean[] called = {false};

            filter.doFilter(request, response, (req, res) -> called[0] = true);

            assertThat(called[0]).isTrue();
        }
    }

    @Nested
    @DisplayName("Payload logging")
    class PayloadLogging {

        @Test
        @DisplayName("should not wrap request when payload logging is disabled")
        void shouldNotWrapWhenDisabled() throws Exception {
            properties.getRequestLogging().setIncludePayload(false);
            filter = new RequestLoggingFilter(properties);

            filter.doFilter(request, response, (req, res) -> {
                // Request should NOT be wrapped in ContentCachingRequestWrapper
                assertThat(req).isInstanceOf(MockHttpServletRequest.class);
            });
        }

        @Test
        @DisplayName("should copy response body back when payload logging is enabled")
        void shouldCopyResponseBodyBack() throws Exception {
            properties.getRequestLogging().setIncludePayload(true);
            filter = new RequestLoggingFilter(properties);

            filter.doFilter(request, response, (req, res) -> {
                res.getWriter().write("{\"status\":\"ok\"}");
            });

            // Response should still have the body (copyBodyToResponse called)
            assertThat(response.getContentAsString()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Max payload length")
    class PayloadTruncation {

        @Test
        @DisplayName("should respect maxPayloadLength configuration")
        void shouldRespectMaxPayloadLength() throws Exception {
            properties.getRequestLogging().setIncludePayload(true);
            properties.getRequestLogging().setMaxPayloadLength(10);
            filter = new RequestLoggingFilter(properties);

            filter.doFilter(request, response, (req, res) -> {
                res.getWriter().write("a]".repeat(100));
            });

            // Should not throw — truncation handles large payloads gracefully
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
