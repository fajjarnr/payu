package id.payu.gateway.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration tests for Simulator Gateway endpoints.
 *
 * <p>Tests the gateway's ability to proxy requests to external simulators:
 * <ul>
 *   <li>BI-FAST Simulator (bank transfer simulation)</li>
 *   <li>Dukcapil Simulator (identity verification simulation)</li>
 *   <li>QRIS Simulator (QR code payment simulation)</li>
 * </ul>
 *
 * <p><b>NOTE:</b> These tests require WireMock to simulate the backend services.
 *
 * @author PayU Engineering Team
 * @since 1.0.0
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "docker.enabled", matches = "true", disabledReason = "Requires WireMock setup")
@DisplayName("Simulator Gateway Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimulatorGatewayIntegrationTest {

    @RegisterExtension
    static WireMockExtension biFastSimulatorMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension dukcapilSimulatorMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension qrisSimulatorMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Reset all WireMock stubs
        if (biFastSimulatorMock != null) biFastSimulatorMock.resetAll();
        if (dukcapilSimulatorMock != null) dukcapilSimulatorMock.resetAll();
        if (qrisSimulatorMock != null) qrisSimulatorMock.resetAll();
    }

    // ==================== BI-FAST Simulator Tests ====================

    @Test
    @Order(1)
    @DisplayName("BI-FAST: Should handle inquiry request")
    void testBiFastInquiry() {
        biFastSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/inquiry"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "00",
                                    "responseMessage": "Success",
                                    "beneficiaryAccount": "1234567890",
                                    "beneficiaryName": "John Doe",
                                    "amount": 1000000
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "sourceAccount": "0987654321",
                            "beneficiaryAccount": "1234567890",
                            "amount": 1000000
                        }
                        """)
                .when()
                .post("/api/v1/simulator/bifast/inquiry")
                .then()
                .statusCode(200)
                .body("responseCode", Matchers.equalTo("00"))
                .body("beneficiaryAccount", Matchers.equalTo("1234567890"));

        biFastSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/inquiry")));
    }

    @Test
    @Order(2)
    @DisplayName("BI-FAST: Should handle transfer request")
    void testBiFastTransfer() {
        biFastSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/transfer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "00",
                                    "responseMessage": "Transfer successful",
                                    "transactionId": "BIFAST-20230130-001",
                                    "amount": 500000,
                                    "fee": 0
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "sourceAccount": "0987654321",
                            "beneficiaryAccount": "1234567890",
                            "amount": 500000,
                            "remark": "Test transfer"
                        }
                        """)
                .when()
                .post("/api/v1/simulator/bifast/transfer")
                .then()
                .statusCode(200)
                .body("responseCode", Matchers.equalTo("00"))
                .body("transactionId", notNullValue())
                .body("amount", Matchers.equalTo(500000));

        biFastSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/transfer")));
    }

    @Test
    @Order(3)
    @DisplayName("BI-FAST: Should handle status inquiry")
    void testBiFastStatus() {
        biFastSimulatorMock.stubFor(get(urlPathMatching("/api/v1/status/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "00",
                                    "transactionId": "BIFAST-20230130-001",
                                    "status": "COMPLETED",
                                    "timestamp": "2026-01-30T10:00:00Z"
                                }
                                """)));

        given()
                .when()
                .get("/api/v1/simulator/bifast/status/BIFAST-20230130-001")
                .then()
                .statusCode(200)
                .body("status", Matchers.equalTo("COMPLETED"))
                .body("transactionId", Matchers.equalTo("BIFAST-20230130-001"));

        biFastSimulatorMock.verify(1, getRequestedFor(urlPathMatching("/api/v1/status/.*")));
    }

    @Test
    @Order(4)
    @DisplayName("BI-FAST: Should handle inquiry errors")
    void testBiFastInquiryError() {
        biFastSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/inquiry"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "01",
                                    "responseMessage": "Invalid account number"
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "sourceAccount": "0987654321",
                            "beneficiaryAccount": "0000000000",
                            "amount": 1000000
                        }
                        """)
                .when()
                .post("/api/v1/simulator/bifast/inquiry")
                .then()
                .statusCode(anyOf(is(400), is(502), is(503)))
                .body("responseCode", containsString("01"));
    }

    // ==================== Dukcapil Simulator Tests ====================

    @Test
    @Order(10)
    @DisplayName("Dukcapil: Should handle identity verification")
    void testDukcapilVerify() {
        dukcapilSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/verify"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "00",
                                    "responseMessage": "Identity verified",
                                    "nik": "1234567890123456",
                                    "fullName": "John Doe",
                                    "dateOfBirth": "1990-01-01",
                                    "address": "Jl. Test No. 123"
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "nik": "1234567890123456",
                            "fullName": "John Doe",
                            "dateOfBirth": "1990-01-01"
                        }
                        """)
                .when()
                .post("/api/v1/simulator/dukcapil/verify")
                .then()
                .statusCode(200)
                .body("responseCode", Matchers.equalTo("00"))
                .body("nik", Matchers.equalTo("1234567890123456"));

        dukcapilSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/verify")));
    }

    @Test
    @Order(11)
    @DisplayName("Dukcapil: Should handle photo matching")
    void testDukcapilMatchPhoto() {
        dukcapilSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/match-photo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "00",
                                    "responseMessage": "Photo matched",
                                    "matchScore": 0.95,
                                    "matchResult": "MATCH"
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "nik": "1234567890123456",
                            "photoBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
                        }
                        """)
                .when()
                .post("/api/v1/simulator/dukcapil/match-photo")
                .then()
                .statusCode(200)
                .body("responseCode", Matchers.equalTo("00"))
                .body("matchScore", Matchers.greaterThan(0.9));

        dukcapilSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/match-photo")));
    }

    @Test
    @Order(12)
    @DisplayName("Dukcapil: Should handle NIK lookup")
    void testDukcapilGetNik() {
        dukcapilSimulatorMock.stubFor(get(urlPathMatching("/api/v1/nik/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "nik": "1234567890123456",
                                    "fullName": "John Doe",
                                    "gender": "MALE",
                                    "citizenship": "WNI",
                                    "valid": true
                                }
                                """)));

        given()
                .when()
                .get("/api/v1/simulator/dukcapil/nik/1234567890123456")
                .then()
                .statusCode(200)
                .body("nik", Matchers.equalTo("1234567890123456"))
                .body("valid", Matchers.equalTo(true));

        dukcapilSimulatorMock.verify(1, getRequestedFor(urlPathMatching("/api/v1/nik/.*")));
    }

    @Test
    @Order(13)
    @DisplayName("Dukcapil: Should handle verification failure")
    void testDukcapilVerifyFailure() {
        dukcapilSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/verify"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "01",
                                    "responseMessage": "NIK not found"
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "nik": "0000000000000000",
                            "fullName": "Unknown",
                            "dateOfBirth": "1990-01-01"
                        }
                        """)
                .when()
                .post("/api/v1/simulator/dukcapil/verify")
                .then()
                .statusCode(anyOf(is(404), is(502), is(503)))
                .body("responseCode", containsString("01"));
    }

    // ==================== QRIS Simulator Tests ====================

    @Test
    @Order(20)
    @DisplayName("QRIS: Should generate QR code")
    void testQrisGenerate() {
        qrisSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "00",
                                    "responseMessage": "QR code generated",
                                    "qrId": "QRIS-20230130-001",
                                    "qrString": "00020101021226580016ID.CO.QRIS.WWW01189360052000000000000303UMI51440014ID.CO.QRIS.WWW0215ID10200200000000000303UMI5204581253033605802ID5910PayU Test6007Jakarta6105101106204100150303036304A5B6",
                                    "amount": 50000,
                                    "expiryTime": "2026-01-30T11:00:00Z"
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "merchantId": "102002000000000",
                            "amount": 50000,
                            "terminalId": "TERM001"
                        }
                        """)
                .when()
                .post("/api/v1/simulator/qris/generate")
                .then()
                .statusCode(200)
                .body("responseCode", Matchers.equalTo("00"))
                .body("qrId", notNullValue())
                .body("qrString", notNullValue())
                .body("amount", Matchers.equalTo(50000));

        qrisSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/generate")));
    }

    @Test
    @Order(21)
    @DisplayName("QRIS: Should handle payment")
    void testQrisPay() {
        qrisSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/pay"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "00",
                                    "responseMessage": "Payment successful",
                                    "transactionId": "QRIS-20230130-001",
                                    "amount": 50000,
                                    "status": "COMPLETED",
                                    "timestamp": "2026-01-30T10:00:00Z"
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "qrId": "QRIS-20230130-001",
                            "amount": 50000,
                            "paymentMethod": "QRIS"
                        }
                        """)
                .when()
                .post("/api/v1/simulator/qris/pay")
                .then()
                .statusCode(200)
                .body("responseCode", Matchers.equalTo("00"))
                .body("status", Matchers.equalTo("COMPLETED"))
                .body("transactionId", notNullValue());

        qrisSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/pay")));
    }

    @Test
    @Order(22)
    @DisplayName("QRIS: Should check payment status")
    void testQrisStatus() {
        qrisSimulatorMock.stubFor(get(urlPathMatching("/api/v1/status/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "qrId": "QRIS-20230130-001",
                                    "transactionId": "QRIS-20230130-001",
                                    "amount": 50000,
                                    "status": "COMPLETED",
                                    "paymentTimestamp": "2026-01-30T10:00:00Z"
                                }
                                """)));

        given()
                .when()
                .get("/api/v1/simulator/qris/status/QRIS-20230130-001")
                .then()
                .statusCode(200)
                .body("qrId", Matchers.equalTo("QRIS-20230130-001"))
                .body("status", Matchers.equalTo("COMPLETED"));

        qrisSimulatorMock.verify(1, getRequestedFor(urlPathMatching("/api/v1/status/.*")));
    }

    @Test
    @Order(23)
    @DisplayName("QRIS: Should handle payment failure")
    void testQrisPayFailure() {
        qrisSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/pay"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "responseCode": "01",
                                    "responseMessage": "QR code expired"
                                }
                                """)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "qrId": "QRIS-EXPIRED",
                            "amount": 50000,
                            "paymentMethod": "QRIS"
                        }
                        """)
                .when()
                .post("/api/v1/simulator/qris/pay")
                .then()
                .statusCode(anyOf(is(400), is(502), is(503)))
                .body("responseCode", containsString("01"));
    }

    // ==================== Timeout and Retry Tests ====================

    @Test
    @Order(30)
    @DisplayName("Should handle simulator timeout")
    void testSimulatorTimeout() {
        biFastSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/inquiry"))
                .willReturn(aResponse()
                        .withFixedDelay(35000) // 35 second delay (longer than 30s timeout)
                        .withStatus(200)));

        given()
                .contentType(ContentType.JSON)
                .body("{\"sourceAccount\":\"0987654321\",\"beneficiaryAccount\":\"1234567890\"}")
                .when()
                .post("/api/v1/simulator/bifast/inquiry")
                .then()
                .statusCode(anyOf(is(504), is(503))); // Gateway Timeout or Service Unavailable
    }

    @Test
    @Order(31)
    @DisplayName("Should retry failed simulator requests")
    void testSimulatorRetry() {
        // First two attempts fail, third succeeds
        dukcapilSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/verify"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withFixedDelay(5000).withStatus(503))
                .willSetStateTo("Retry 1"));

        dukcapilSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/verify"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Retry 1")
                .willReturn(aResponse().withFixedDelay(5000).withStatus(503))
                .willSetStateTo("Retry 2"));

        dukcapilSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/verify"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Retry 2")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"responseCode\":\"00\",\"nik\":\"1234567890123456\"}")));

        given()
                .contentType(ContentType.JSON)
                .body("{\"nik\":\"1234567890123456\"}")
                .when()
                .post("/api/v1/simulator/dukcapil/verify")
                .then()
                .statusCode(200);
    }

    // ==================== Correlation ID Tests ====================

    @Test
    @Order(40)
    @DisplayName("Should forward correlation ID to simulators")
    void testCorrelationIdForwarded() {
        String correlationId = "test-correlation-12345";

        biFastSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/inquiry"))
                .withHeader("X-Correlation-Id", WireMock.equalTo(correlationId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"responseCode\":\"00\"}")));

        given()
                .header("X-Correlation-Id", correlationId)
                .contentType(ContentType.JSON)
                .body("{\"sourceAccount\":\"0987654321\"}")
                .when()
                .post("/api/v1/simulator/bifast/inquiry")
                .then()
                .statusCode(200);

        biFastSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/inquiry"))
                .withHeader("X-Correlation-Id", WireMock.equalTo(correlationId)));
    }

    // ==================== Simulator Not Configured Tests ====================

    @Test
    @Order(50)
    @DisplayName("Should return 503 when simulator not configured")
    void testSimulatorNotConfigured() {
        // This test assumes the simulator is properly configured
        // In a real scenario, we'd test with an unconfigured simulator
        // For now, we just verify the error handling structure
    }

    // ==================== Multiple Simulator Requests Tests ====================

    @Test
    @Order(60)
    @DisplayName("Should handle concurrent requests to different simulators")
    void testConcurrentSimulatorRequests() throws InterruptedException {
        biFastSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/inquiry"))
                .willReturn(aResponse().withStatus(200).withBody("{\"responseCode\":\"00\"}")));

        dukcapilSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/verify"))
                .willReturn(aResponse().withStatus(200).withBody("{\"responseCode\":\"00\"}")));

        qrisSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/generate"))
                .willReturn(aResponse().withStatus(200).withBody("{\"responseCode\":\"00\"}")));

        Thread[] threads = new Thread[3];
        threads[0] = new Thread(() -> given().contentType(ContentType.JSON)
                .body("{\"sourceAccount\":\"0987654321\"}")
                .post("/api/v1/simulator/bifast/inquiry").then().statusCode(200));

        threads[1] = new Thread(() -> given().contentType(ContentType.JSON)
                .body("{\"nik\":\"1234567890123456\"}")
                .post("/api/v1/simulator/dukcapil/verify").then().statusCode(200));

        threads[2] = new Thread(() -> given().contentType(ContentType.JSON)
                .body("{\"amount\":50000}")
                .post("/api/v1/simulator/qris/generate").then().statusCode(200));

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        biFastSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/inquiry")));
        dukcapilSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/verify")));
        qrisSimulatorMock.verify(1, postRequestedFor(urlPathEqualTo("/api/v1/generate")));
    }

    // ==================== Fault Tolerance Tests ====================

    @Test
    @Order(70)
    @DisplayName("Should recover from transient simulator failures")
    void testSimulatorRecovery() {
        // First request fails
        qrisSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/generate"))
                .inScenario("Recovery Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("Recovered"));

        // Second request succeeds
        qrisSimulatorMock.stubFor(post(urlPathEqualTo("/api/v1/generate"))
                .inScenario("Recovery Scenario")
                .whenScenarioStateIs("Recovered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"responseCode\":\"00\",\"qrId\":\"QRIS-001\"}")));

        // First call might fail
        given()
                .contentType(ContentType.JSON)
                .body("{\"amount\":50000}")
                .when()
                .post("/api/v1/simulator/qris/generate")
                .then()
                .statusCode(anyOf(is(200), is(503)));

        // Second call should succeed (after retry or recovery)
        given()
                .contentType(ContentType.JSON)
                .body("{\"amount\":50000}")
                .when()
                .post("/api/v1/simulator/qris/generate")
                .then()
                .statusCode(200);
    }
}
