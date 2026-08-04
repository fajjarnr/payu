package id.payu.partner.adapter.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletSettlementAdapterTest {

    private HttpServer server;
    private AtomicBoolean walletCallsUseServiceToken;
    private AtomicInteger tokenRequests;

    @BeforeEach
    void setUp() throws IOException {
        walletCallsUseServiceToken = new AtomicBoolean(true);
        tokenRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/realms/payu/protocol/openid-connect/token", exchange -> {
            tokenRequests.incrementAndGet();
            respond(exchange, 200, "{\"access_token\":\"service-token\",\"expires_in\":300}");
        });
        server.createContext("/api/v1/wallets/ACC-001/reserve", this::checkServiceToken);
        server.createContext("/api/v1/wallets/reservations/res-1/commit", this::checkServiceToken);
        server.createContext("/api/v1/wallets/ACC-002/credit", this::checkServiceToken);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void settlesWithPlatformTokenAndCachesItAcrossWalletCalls() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        WalletSettlementAdapter adapter = new WalletSettlementAdapter(
                baseUrl,
                baseUrl,
                "payu",
                "payu-backend",
                "backend-secret");

        adapter.settle("ACC-001", "ACC-002", java.math.BigDecimal.ONE, "IDR", "snap-ref-1");

        assertTrue(walletCallsUseServiceToken.get());
        assertEquals(1, tokenRequests.get());
    }

    private void checkServiceToken(HttpExchange exchange) throws IOException {
        walletCallsUseServiceToken.set(
                "Bearer service-token".equals(exchange.getRequestHeaders().getFirst("Authorization")));
        String body = exchange.getRequestURI().getPath().endsWith("reserve")
                ? "{\"data\":{\"reservationId\":\"res-1\"}}"
                : "{}";
        respond(exchange, 200, body);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
