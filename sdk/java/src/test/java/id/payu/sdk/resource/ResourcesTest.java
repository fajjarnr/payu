package id.payu.sdk.resource;

import id.payu.sdk.PayUClient;
import id.payu.sdk.error.PayUError;
import id.payu.sdk.error.PayUException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcesTest {

    private MockWebServer server;
    private PayUClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = PayUClient.builder()
                .apiKey("test-key")
                .apiSecret("test-secret")
                .baseUrl(server.url("/").toString().replaceAll("/$", ""))
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void paymentsCreateHitsCorrectPathWithIdempotencyHeader() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"data\":{\"id\":\"p1\"}}").addHeader("Content-Type", "application/json"));

        Map<?, ?> response = client.payments().create("{\"amount\":1000}", "idem-1", Map.class);

        RecordedRequest request = server.takeRequest();
        assertEquals("/api/v1/payments", request.getPath());
        assertEquals("idem-1", request.getHeader("X-Idempotency-Key"));
        assertEquals("test-key", request.getHeader("X-API-Key"));
        assertEquals("p1", ((Map<?, ?>) response.get("data")).get("id"));
    }

    @Test
    void walletsGetBalanceHitsBalanceEndpoint() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"data\":{\"availableBalance\":\"1000.0000\"}}"));

        Map<?, ?> response = client.wallets().getBalance("acc-1", Map.class);

        assertEquals("/api/v1/wallets/acc-1/balance", server.takeRequest().getPath());
        assertEquals("1000.0000", ((Map<?, ?>) response.get("data")).get("availableBalance"));
    }

    @Test
    void transfersCreateHitsTransferEndpoint() throws Exception {
        server.enqueue(new MockResponse().setBody("{}"));

        client.transfers().create("{\"amount\":100}", "idem-2", Map.class);

        assertEquals("/api/v1/transactions/transfer", server.takeRequest().getPath());
    }

    @Test
    void nonSuccessfulResponseThrowsPayUError() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"unauthorized\"}"));

        PayUException thrown = assertThrows(PayUException.class,
                () -> client.wallets().getBalance("acc-1", Map.class));

        assertTrue(thrown instanceof PayUError);
        assertEquals(401, ((PayUError) thrown).getStatusCode());
    }
}
