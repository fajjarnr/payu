package id.payu.gateway.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CorrelationIdFilter.
 * Tests correlation ID generation and propagation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

    @InjectMocks
    private CorrelationIdFilter correlationIdFilter;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    @Mock
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/v1/accounts");
        when(requestContext.getMethod()).thenReturn("GET");
    }

    @Nested
    @DisplayName("Request Filter")
    class RequestFilter {

        @Test
        @DisplayName("should generate new correlation ID if not present in header")
        void shouldGenerateNewCorrelationIdIfNotPresent() {
            // Given
            when(requestContext.getHeaderString(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);

            // When
            correlationIdFilter.filter(requestContext);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
            verify(requestContext, times(3)).setProperty(keyCaptor.capture(), valueCaptor.capture());

            // Verify correlation ID was stored
            assertThat(keyCaptor.getAllValues()).contains(CorrelationIdFilter.CORRELATION_ID_HEADER);
        }

        @Test
        @DisplayName("should preserve existing correlation ID from header")
        void shouldPreserveExistingCorrelationId() {
            // Given
            String existingCorrelationId = "existing-correlation-id-123";
            when(requestContext.getHeaderString(CorrelationIdFilter.CORRELATION_ID_HEADER))
                    .thenReturn(existingCorrelationId);

            // When
            correlationIdFilter.filter(requestContext);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
            verify(requestContext, times(3)).setProperty(keyCaptor.capture(), valueCaptor.capture());

            // Find the correlation ID that was stored
            int correlationIdIndex = keyCaptor.getAllValues().indexOf(CorrelationIdFilter.CORRELATION_ID_HEADER);
            assertThat(correlationIdIndex).isGreaterThanOrEqualTo(0);
            assertThat(valueCaptor.getAllValues().get(correlationIdIndex)).isEqualTo(existingCorrelationId);
        }

        @Test
        @DisplayName("should generate correlation ID if header is blank")
        void shouldGenerateCorrelationIdIfBlank() {
            // Given
            when(requestContext.getHeaderString(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("   ");

            // When
            correlationIdFilter.filter(requestContext);

            // Then
            ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
            verify(requestContext, atLeastOnce()).setProperty(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), valueCaptor.capture());
            
            // A new UUID should be generated (not blank)
            String generatedId = (String) valueCaptor.getValue();
            assertThat(generatedId).isNotBlank();
        }

        @Test
        @DisplayName("should always generate a new request ID")
        void shouldAlwaysGenerateRequestId() {
            // Given
            when(requestContext.getHeaderString(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);

            // When
            correlationIdFilter.filter(requestContext);

            // Then
            verify(requestContext).setProperty(eq(CorrelationIdFilter.REQUEST_ID_HEADER), any(String.class));
        }
    }

    @Nested
    @DisplayName("Response Filter")
    class ResponseFilter {

        @Test
        @DisplayName("should add correlation ID to response headers")
        void shouldAddCorrelationIdToResponseHeaders() {
            // Given
            String correlationId = "test-correlation-id";
            when(requestContext.getProperty(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(correlationId);
            when(requestContext.getProperty(CorrelationIdFilter.REQUEST_ID_HEADER)).thenReturn("req-123");
            when(requestContext.getProperty("request-start-time")).thenReturn(System.currentTimeMillis() - 100);
            
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(responseContext.getHeaders()).thenReturn(headers);
            when(responseContext.getStatus()).thenReturn(200);

            // When
            correlationIdFilter.filter(requestContext, responseContext);

            // Then
            assertThat(headers.getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(correlationId);
        }

        @Test
        @DisplayName("should add request ID to response headers")
        void shouldAddRequestIdToResponseHeaders() {
            // Given
            String requestId = "req-test-123";
            when(requestContext.getProperty(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("corr-id");
            when(requestContext.getProperty(CorrelationIdFilter.REQUEST_ID_HEADER)).thenReturn(requestId);
            when(requestContext.getProperty("request-start-time")).thenReturn(System.currentTimeMillis() - 100);
            
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(responseContext.getHeaders()).thenReturn(headers);
            when(responseContext.getStatus()).thenReturn(200);

            // When
            correlationIdFilter.filter(requestContext, responseContext);

            // Then
            assertThat(headers.getFirst(CorrelationIdFilter.REQUEST_ID_HEADER)).isEqualTo(requestId);
        }

        @Test
        @DisplayName("should handle null correlation ID gracefully")
        void shouldHandleNullCorrelationIdGracefully() {
            // Given
            when(requestContext.getProperty(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);
            when(requestContext.getProperty(CorrelationIdFilter.REQUEST_ID_HEADER)).thenReturn(null);
            when(requestContext.getProperty("request-start-time")).thenReturn(null);
            
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            when(responseContext.getHeaders()).thenReturn(headers);

            // When - should not throw
            correlationIdFilter.filter(requestContext, responseContext);

            // Then
            assertThat(headers.containsKey(CorrelationIdFilter.CORRELATION_ID_HEADER)).isFalse();
        }
    }
}
