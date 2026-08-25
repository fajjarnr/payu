package id.payu.partner;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PARTNER-PROD-010: Consumer Pact for partner onboarding.
 * Covers POST /v1/partners with X-Idempotency-Key -> 201 and GET /v1/partners/{id} -> 200.
 * Pact file is written to {@code target/pacts/partner-portal-partner-service.json}
 * and used by provider verification (pact:verify / pact-verify-task).
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "partner-service")
public class PactPartnerOnboardingConsumerTest {

    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";
    private static final String AUTH = "Bearer test-token";

    @Pact(consumer = "partner-portal", provider = "partner-service")
    public V4Pact partnerOnboardingPact(PactBuilder builder) {
        DslPart createRequest = new PactDslJsonBody()
                .stringType("name", "Acme Corp")
                .stringType("type", "MERCHANT")
                .stringType("email", "partner@acme.co.id")
                .stringType("phone", "+62123456789");

        DslPart createResponse = new PactDslJsonBody()
                .booleanType("success", true)
                .object("data")
                    .integerType("id", 1)
                    .stringType("name", "Acme Corp")
                    .stringType("type", "MERCHANT")
                    .stringType("email", "partner@acme.co.id")
                    .stringType("status", "PENDING_APPROVAL")
                    .stringType("clientId")
                    .closeObject()
                .object("meta")
                    .stringType("requestId")
                    .stringType("timestamp")
                    .closeObject();

        DslPart getResponse = new PactDslJsonBody()
                .booleanType("success", true)
                .object("data")
                    .integerType("id", 1)
                    .stringType("name", "Acme Corp")
                    .stringType("type", "MERCHANT")
                    .stringType("email", "partner@acme.co.id")
                    .stringType("status", "PENDING_APPROVAL")
                    .closeObject()
                .object("meta")
                    .stringType("requestId")
                    .stringType("timestamp")
                    .closeObject();

        return builder
                .given("partner onboarding available")
                .expectsToReceiveHttpInteraction(
                        "create partner with X-Idempotency-Key returns 201",
                        http -> http
                                .withRequest(req -> req
                                        .method("POST")
                                        .path("/v1/partners")
                                        .headers(Map.of(
                                                "Content-Type", "application/json",
                                                "X-Idempotency-Key", IDEMPOTENCY_KEY,
                                                "Authorization", AUTH))
                                        .body(createRequest))
                                .willRespondWith(res -> res
                                        .status(201)
                                        .headers(Map.of("Content-Type", "application/json"))
                                        .body(createResponse))
                )
                .given("partner 1 exists")
                .expectsToReceiveHttpInteraction(
                        "get partner by id returns 200",
                        http -> http
                                .withRequest(req -> req
                                        .method("GET")
                                        .path("/v1/partners/1")
                                        .headers(Map.of(
                                                "Authorization", AUTH,
                                                "X-Idempotency-Key", IDEMPOTENCY_KEY)))
                                .willRespondWith(res -> res
                                        .status(200)
                                        .headers(Map.of("Content-Type", "application/json"))
                                        .body(getResponse))
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "partnerOnboardingPact")
    void pactCreateAndGetPartner(MockServer mockServer) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String createBody = """
                {"name":"Acme Corp","type":"MERCHANT","email":"partner@acme.co.id","phone":"+62123456789"}
                """;
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/v1/partners"))
                .header("Content-Type", "application/json")
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("Authorization", AUTH)
                .POST(HttpRequest.BodyPublishers.ofString(createBody))
                .build();
        HttpResponse<String> postResp = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postResp.statusCode(), "POST must return 201");
        assertTrue(postResp.body().contains("PENDING_APPROVAL"));

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/v1/partners/1"))
                .header("Authorization", AUTH)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .GET()
                .build();
        HttpResponse<String> getResp = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResp.statusCode(), "GET must return 200");
        assertTrue(getResp.body().contains("\"id\""));
    }
}
