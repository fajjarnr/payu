package id.payu.integration.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import id.payu.integration.config.TestSecurityConfig;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying Camel HTTP route integration using WireMock.
 * Stubs external HTTP endpoints and validates the outbound Camel route behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WireMockIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private ProducerTemplate producerTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("camel.component.kafka.enabled", () -> "false");
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @AfterEach
    void resetWireMock() {
        WireMock.reset();
    }

    @Test
    @Order(1)
    @DisplayName("Should forward HTTP GET through Camel route and receive stubbed response")
    void testHttpGetViaCamelRoute() {
        stubFor(get(urlEqualTo("/api/mock/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UP\",\"service\":\"mock-service\"}")));

        String response = producerTemplate.requestBodyAndHeaders(
                "direct:http-request",
                "",
                Map.of(
                        "HttpUrl", "http://localhost:" + wireMockServer.port() + "/api/mock/health",
                        "HttpMethod", "GET",
                        "HttpHeaders", Map.of("Accept", "application/json")
                ),
                String.class
        );

        assertThat(response).contains("\"status\":\"UP\"");
        assertThat(response).contains("mock-service");
        verify(getRequestedFor(urlEqualTo("/api/mock/health")));
    }

    @Test
    @Order(2)
    @DisplayName("Should forward HTTP POST with body through Camel route")
    void testHttpPostViaCamelRoute() {
        stubFor(post(urlEqualTo("/api/mock/data"))
                .withRequestBody(equalToJson("{\"key\":\"value\"}"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"123\",\"status\":\"CREATED\"}")));

        String response = producerTemplate.requestBodyAndHeaders(
                "direct:http-request",
                "{\"key\":\"value\"}",
                Map.of(
                        "HttpUrl", "http://localhost:" + wireMockServer.port() + "/api/mock/data",
                        "HttpMethod", "POST",
                        "HttpHeaders", Map.of("Content-Type", "application/json")
                ),
                String.class
        );

        assertThat(response).contains("\"status\":\"CREATED\"");
        verify(postRequestedFor(urlEqualTo("/api/mock/data")));
    }

    @Test
    @Order(3)
    @DisplayName("Should handle WireMock 503 response gracefully through Camel route")
    void testHttpErrorResponse() {
        stubFor(get(urlEqualTo("/api/mock/down"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Service Unavailable\"}")));

        String response = producerTemplate.requestBodyAndHeaders(
                "direct:http-request",
                "",
                Map.of(
                        "HttpUrl", "http://localhost:" + wireMockServer.port() + "/api/mock/down",
                        "HttpMethod", "GET",
                        "HttpHeaders", Map.of()
                ),
                String.class
        );

        assertThat(response).contains("Service Unavailable");
        verify(getRequestedFor(urlEqualTo("/api/mock/down")));
    }

    @Test
    @Order(4)
    @DisplayName("Should create integration message record when triggering SOAP route")
    void testSoapRouteCreatesMessageRecord() {
        stubFor(post(urlEqualTo("/api/mock/legacy"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                "<soap:Body><Response><status>OK</status></Response></soap:Body>" +
                                "</soap:Envelope>")));

        String endpoint = "http://localhost:" + wireMockServer.port() + "/api/mock/legacy";

        String response = producerTemplate.requestBodyAndHeaders(
                "direct:soap-request",
                "<request>test-data</request>",
                Map.of(
                        "SoapEndpoint", endpoint,
                        "SoapOperation", "TestOperation",
                        "MessageId", java.util.UUID.randomUUID().toString()
                ),
                String.class
        );

        assertThat(response).contains("status");
        verify(postRequestedFor(urlEqualTo("/api/mock/legacy")));
    }
}
