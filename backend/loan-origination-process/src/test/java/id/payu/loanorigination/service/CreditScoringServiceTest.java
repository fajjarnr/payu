package id.payu.loanorigination.service;

import id.payu.shared.restclient.PayuRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditScoringServiceTest {

    @Mock
    private PayuRestClient restClient;

    private CreditScoringService service(String url) {
        return new CreditScoringService(restClient, url);
    }

    @Test
    void returnsBigDecimalScore() {
        when(restClient.post(eq("lending-rules"), any(), any(), any(Class.class))).thenReturn(
                ResponseEntity.ok(Map.of("score", new BigDecimal("700"))));

        assertThat(service("http://rules:8080").evaluate(new BigDecimal("100000"), 12))
                .isEqualTo(new BigDecimal("700"));
    }

    @Test
    void returnsNumberScore() {
        when(restClient.post(eq("lending-rules"), any(), any(), any(Class.class))).thenReturn(
                ResponseEntity.ok(Map.of("score", 700)));

        assertThat(service("http://rules:8080").evaluate(new BigDecimal("100000"), 12))
                .isEqualByComparingTo("700");
    }

    @Test
    void returnsStringScore() {
        when(restClient.post(eq("lending-rules"), any(), any(), any(Class.class))).thenReturn(
                ResponseEntity.ok(Map.of("score", "600")));

        assertThat(service("http://rules:8080").evaluate(new BigDecimal("100000"), 12))
                .isEqualByComparingTo("600");
    }

    @Test
    void defaultsToZeroWhenNoScoreInBody() {
        when(restClient.post(eq("lending-rules"), any(), any(), any(Class.class))).thenReturn(
                ResponseEntity.ok(Map.of()));

        assertThat(service("http://rules:8080").evaluate(new BigDecimal("100000"), 12))
                .isEqualByComparingTo("0");
    }

    @Test
    void defaultsToZeroWhenCallFails() {
        when(restClient.post(eq("lending-rules"), any(), any(), any(Class.class))).thenThrow(
                new RuntimeException("rules down"));

        assertThat(service("http://rules:8080").evaluate(new BigDecimal("100000"), 12))
                .isEqualByComparingTo("0");
    }
}
