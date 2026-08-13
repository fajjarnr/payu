package id.payu.integration.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import id.payu.integration.config.TestSecurityConfig;
import id.payu.outbox.service.OutboxService;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=id.payu.outbox.config.OutboxAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "spring.flyway.enabled=false"
    }
)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class OjkRouteBuilderIntegrationTest {

    @MockitoBean
    private OutboxService outboxService;

    private static WireMockServer wireMockServer;

    @Autowired
    private ProducerTemplate producerTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("payu.integration.ojk.upload-url", () -> "http://localhost:" + wireMockServer.port() + "/api/v1/upload");
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
    @DisplayName("Should successfully generate and upload OJK daily CSV report")
    void testDailyCsvReport() {
        stubFor(post(urlEqualTo("/api/v1/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("UPLOADED")));

        producerTemplate.sendBodyAndHeaders("direct:ojk-csv-report", null, Map.of(
                "reportType", "DAILY_CSV",
                "reportDate", "2024-01-01"
        ));

        verify(postRequestedFor(urlEqualTo("/api/v1/upload"))
                .withHeader("Content-Type", equalTo("text/csv"))
                .withRequestBody(containing("ReportDate,ReportType,InstitutionCode,TotalTransactions,TotalAmount,Currency")));
    }

    @Test
    @DisplayName("Should successfully generate and upload OJK monthly XML report")
    void testMonthlyXmlReport() {
        stubFor(post(urlEqualTo("/api/v1/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("UPLOADED")));

        producerTemplate.sendBodyAndHeaders("direct:ojk-xml-report", null, Map.of(
                "reportType", "MONTHLY_XML",
                "reportDate", "2024-01-01"
        ));

        verify(postRequestedFor(urlEqualTo("/api/v1/upload"))
                .withHeader("Content-Type", equalTo("application/xml"))
                .withRequestBody(containing("<OJKReport")));
    }
}
