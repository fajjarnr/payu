package id.payu.shared.restclient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RestClientErrorHandler")
class RestClientErrorHandlerTest {

    private final RestClientErrorHandler handler = new RestClientErrorHandler();

    private ClientHttpResponse response(HttpStatus status) throws IOException {
        ClientHttpResponse r = mock(ClientHttpResponse.class);
        when(r.getStatusCode()).thenReturn(status);
        when(r.getStatusText()).thenReturn(status.getReasonPhrase());
        when(r.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
        return r;
    }

    @Test
    @DisplayName("hasError returns true for 5xx and false for 2xx")
    void hasErrorDistinguishesStatus() throws IOException {
        assertThat(handler.hasError(response(HttpStatus.INTERNAL_SERVER_ERROR))).isTrue();
        assertThat(handler.hasError(response(HttpStatus.NOT_FOUND))).isTrue();
        assertThat(handler.hasError(response(HttpStatus.OK))).isFalse();
    }

    @Test
    @DisplayName("429 maps to retryable ExternalServiceUnavailableException with EXT_RATE_LIMITED")
    void rateLimitedMapsToUnavailable() throws IOException {
        assertThatThrownBy(() -> handler.handleError(null, null, response(HttpStatus.TOO_MANY_REQUESTS)))
                .isInstanceOf(RestClientErrorHandler.ExternalServiceUnavailableException.class)
                .hasMessageContaining("HTTP 429")
                .satisfies(e -> assertThat(((RestClientErrorHandler.ExternalServiceUnavailableException) e).getCode())
                        .isEqualTo("EXT_RATE_LIMITED"));
    }

    @Test
    @DisplayName("5xx maps to retryable ExternalServiceUnavailableException with EXT_SERVER_ERROR")
    void serverErrorMapsToUnavailable() throws IOException {
        assertThatThrownBy(() -> handler.handleError(null, null, response(HttpStatus.BAD_GATEWAY)))
                .isInstanceOf(RestClientErrorHandler.ExternalServiceUnavailableException.class)
                .hasMessageContaining("HTTP 502")
                .satisfies(e -> assertThat(((RestClientErrorHandler.ExternalServiceUnavailableException) e).getCode())
                        .isEqualTo("EXT_SERVER_ERROR"));
    }

    @Test
    @DisplayName("401/403 map to non-retryable client error with EXT_AUTH_ERROR")
    void authErrorMapsToClientError() throws IOException {
        assertThatThrownBy(() -> handler.handleError(null, null, response(HttpStatus.UNAUTHORIZED)))
                .isInstanceOf(RestClientErrorHandler.ExternalServiceClientException.class)
                .hasMessageContaining("HTTP 401")
                .satisfies(e -> assertThat(((RestClientErrorHandler.ExternalServiceClientException) e).getCode())
                        .isEqualTo("EXT_AUTH_ERROR"));
    }

    @Test
    @DisplayName("404 maps to non-retryable client error with EXT_NOT_FOUND")
    void notFoundMapsToClientError() throws IOException {
        assertThatThrownBy(() -> handler.handleError(null, null, response(HttpStatus.NOT_FOUND)))
                .isInstanceOf(RestClientErrorHandler.ExternalServiceClientException.class)
                .hasMessageContaining("HTTP 404")
                .satisfies(e -> assertThat(((RestClientErrorHandler.ExternalServiceClientException) e).getCode())
                        .isEqualTo("EXT_NOT_FOUND"));
    }

    @Test
    @DisplayName("other 4xx maps to EXT_CLIENT_ERROR")
    void otherClientErrorMapsToClientError() throws IOException {
        assertThatThrownBy(() -> handler.handleError(null, null, response(HttpStatus.CONFLICT)))
                .isInstanceOf(RestClientErrorHandler.ExternalServiceClientException.class)
                .hasMessageContaining("HTTP 409")
                .satisfies(e -> assertThat(((RestClientErrorHandler.ExternalServiceClientException) e).getCode())
                        .isEqualTo("EXT_CLIENT_ERROR"));
    }

    @Test
    @DisplayName("unavailable and client exceptions expose their error code")
    void exceptionsExposeCode() {
        assertThat(new RestClientErrorHandler.ExternalServiceUnavailableException("X", "m").getCode()).isEqualTo("X");
        assertThat(new RestClientErrorHandler.ExternalServiceClientException("Y", "m").getCode()).isEqualTo("Y");
    }
}
