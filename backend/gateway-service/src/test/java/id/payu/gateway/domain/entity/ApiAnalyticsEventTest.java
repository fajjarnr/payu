package id.payu.gateway.domain.entity;

import id.payu.gateway.domain.vo.HttpMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiAnalyticsEvent domain entity.
 */
class ApiAnalyticsEventTest {

    @Test
    void shouldCreateEventWithBuilder() {
        ApiAnalyticsEvent event = ApiAnalyticsEvent.builder()
            .id("event-1")
            .partnerId("partner-1")
            .endpoint("/api/v1/accounts")
            .method("GET")
            .statusCode(200)
            .durationMs(150)
            .requestSize(100)
            .responseSize(500)
            .clientIp("192.168.1.1")
            .userAgent("TestAgent")
            .correlationId("corr-123")
            .build();

        assertEquals("event-1", event.getId());
        assertEquals("partner-1", event.getPartnerId());
        assertEquals("/api/v1/accounts", event.getEndpoint());
        assertEquals(HttpMethod.GET, event.getMethod());
        assertEquals(200, event.getStatusCode());
        assertEquals(150, event.getDurationMs());
        assertTrue(event.isSuccess());
        assertFalse(event.isError());
    }

    @Test
    void shouldDetectErrorStatus() {
        ApiAnalyticsEvent event = ApiAnalyticsEvent.builder()
            .endpoint("/api/v1/accounts")
            .method("GET")
            .statusCode(404)
            .durationMs(50)
            .build();

        assertTrue(event.isError());
        assertFalse(event.isSuccess());
        assertFalse(event.isServerError());
    }

    @Test
    void shouldDetectServerError() {
        ApiAnalyticsEvent event = ApiAnalyticsEvent.builder()
            .endpoint("/api/v1/accounts")
            .method("GET")
            .statusCode(500)
            .durationMs(100)
            .build();

        assertTrue(event.isError());
        assertTrue(event.isServerError());
        assertFalse(event.isSuccess());
    }

    @Test
    void shouldGenerateEndpointKey() {
        ApiAnalyticsEvent event = ApiAnalyticsEvent.builder()
            .endpoint("/api/v1/accounts")
            .method("POST")
            .statusCode(201)
            .durationMs(200)
            .build();

        assertEquals("POST:/api/v1/accounts", event.getEndpointKey());
    }

    @Test
    void shouldGeneratePartnerEndpointKey() {
        ApiAnalyticsEvent event = ApiAnalyticsEvent.builder()
            .partnerId("partner-1")
            .endpoint("/api/v1/accounts")
            .method("GET")
            .statusCode(200)
            .durationMs(100)
            .build();

        assertEquals("partner-1:GET:/api/v1/accounts", event.getPartnerEndpointKey());
    }

    @Test
    void shouldUseAnonymousForNullPartner() {
        ApiAnalyticsEvent event = ApiAnalyticsEvent.builder()
            .endpoint("/api/v1/accounts")
            .method("GET")
            .statusCode(200)
            .durationMs(100)
            .build();

        assertEquals("anonymous:GET:/api/v1/accounts", event.getPartnerEndpointKey());
    }

    @Test
    void shouldThrowExceptionForInvalidStatusCode() {
        assertThrows(IllegalArgumentException.class, () ->
            ApiAnalyticsEvent.builder()
                .endpoint("/api/v1/accounts")
                .method("GET")
                .statusCode(99)
                .durationMs(100)
                .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
            ApiAnalyticsEvent.builder()
                .endpoint("/api/v1/accounts")
                .method("GET")
                .statusCode(600)
                .durationMs(100)
                .build()
        );
    }

    @Test
    void shouldThrowExceptionForNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () ->
            ApiAnalyticsEvent.builder()
                .endpoint("/api/v1/accounts")
                .method("GET")
                .statusCode(200)
                .durationMs(-1)
                .build()
        );
    }

    @Test
    void shouldThrowExceptionForNegativeRequestSize() {
        assertThrows(IllegalArgumentException.class, () ->
            ApiAnalyticsEvent.builder()
                .endpoint("/api/v1/accounts")
                .method("GET")
                .statusCode(200)
                .durationMs(100)
                .requestSize(-1)
                .build()
        );
    }

    @Test
    void shouldAutoGenerateIdAndTimestamp() {
        ApiAnalyticsEvent event = ApiAnalyticsEvent.builder()
            .endpoint("/api/v1/accounts")
            .method("GET")
            .statusCode(200)
            .durationMs(100)
            .build();

        assertNotNull(event.getId());
        assertNotNull(event.getTimestamp());
    }
}
