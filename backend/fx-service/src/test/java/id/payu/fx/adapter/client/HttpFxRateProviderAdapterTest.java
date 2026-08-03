package id.payu.fx.adapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpFxRateProviderAdapterTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldParseAndAuditFreshProviderRate() {
        server.createContext("/latest", exchange -> {
            String response = "{\"base\":\"IDR\",\"rates\":{\"USD\":0.000061},"
                    + "\"timestamp\":\"" + Instant.now() + "\",\"source\":\"approved-provider\"}";
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();

        HttpFxRateProviderAdapter adapter = adapter();
        var result = adapter.fetchCurrentRate("IDR", "USD");

        assertThat(result.getRate()).isEqualByComparingTo(new BigDecimal("0.000061"));
        assertThat(result.getSource()).isEqualTo("approved-provider");
        assertThat(result.getObservedAt()).isNotNull();
    }

    @Test
    void shouldRejectStaleProviderRate() {
        server.createContext("/latest", exchange -> {
            String response = "{\"base\":\"IDR\",\"rates\":{\"USD\":0.000061},"
                    + "\"timestamp\":\"" + Instant.now().minus(Duration.ofHours(1)) + "\"}";
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> adapter().fetchCurrentRate("IDR", "USD"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FX provider rate is stale");
    }

    private HttpFxRateProviderAdapter adapter() {
        return new HttpFxRateProviderAdapter(
                new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/latest",
                "",
                "configured-source",
                Duration.ofSeconds(2),
                Duration.ofMinutes(15));
    }
}
