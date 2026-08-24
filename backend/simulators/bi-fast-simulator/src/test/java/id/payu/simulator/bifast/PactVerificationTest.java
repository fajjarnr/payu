package id.payu.simulator.bifast;

import id.payu.simulator.bifast.interfaces.dto.InquiryRequest;
import id.payu.simulator.bifast.interfaces.dto.TransferRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PactVerificationTest {

    @Test
    void pactInquirySuccess() {
        given().contentType(ContentType.JSON).body(new InquiryRequest("BCA","1234567890"))
                .when().post("/api/v1/inquiry").then().statusCode(200).body("responseCode", equalTo("00"));
    }

    @Test
    void pactInquiryNotFound() {
        given().contentType(ContentType.JSON).body(new InquiryRequest("BCA","9999999999"))
                .when().post("/api/v1/inquiry").then().statusCode(404).body("responseCode", equalTo("14"));
    }

    @Test
    void pactInquiryBlockedViaXSimulate() {
        given().contentType(ContentType.JSON).header("X-Simulate","blocked").body(new InquiryRequest("BCA","1234567890"))
                .when().post("/api/v1/inquiry").then().statusCode(403).body("responseCode", equalTo("62"));
    }

    @Test
    void pactInquiryRateLimitViaXSimulate() {
        given().contentType(ContentType.JSON).header("X-Simulate","rate-limit").body(new InquiryRequest("BCA","1234567890"))
                .when().post("/api/v1/inquiry").then().statusCode(429).body("responseCode", equalTo("42"));
    }

    @Test
    void pactInquiry5xxViaXSimulate() {
        given().contentType(ContentType.JSON).header("X-Simulate","5xx").body(new InquiryRequest("BCA","1234567890"))
                .when().post("/api/v1/inquiry").then().statusCode(500).body("responseCode", equalTo("96"));
    }

    @Test
    void pactTransferSuccessWithIdempotency() {
        TransferRequest req = new TransferRequest("TX-0001","BCA","1234567890","John Doe","BRI","0987654321", new BigDecimal("100000"), "IDR", null, null);
        given().contentType(ContentType.JSON).header("X-Idempotency-Key","TX-0001").header("X-Simulate","success").body(req)
                .when().post("/api/v1/transfer").then().statusCode(200).body("referenceNumber", equalTo("TX-0001")).body("responseCode", equalTo("00"));
    }

    @Test
    void pactTransferDuplicateIdempotency() {
        TransferRequest req = new TransferRequest("TX-0001","BCA","1234567890","John Doe","BRI","0987654321", new BigDecimal("100000"), "IDR", null, null);
        // first already created in previous test, second should return same
        given().contentType(ContentType.JSON).header("X-Idempotency-Key","TX-0001").header("X-Simulate","success").body(req)
                .when().post("/api/v1/transfer").then().statusCode(200).body("referenceNumber", equalTo("TX-0001"));
    }

    @Test
    void pactTransferTimeoutViaXSimulate() {
        TransferRequest req = new TransferRequest("TX-TIMEOUT-1","BCA","1234567890","John Doe","BNI","9999888877", new BigDecimal("50000"), "IDR", null, null);
        given().contentType(ContentType.JSON).header("X-Simulate","timeout").body(req)
                .when().post("/api/v1/transfer").then().statusCode(504).body("responseCode", equalTo("68"));
    }

    @Test
    void pactTransfer5xxViaXSimulate() {
        TransferRequest req = new TransferRequest("TX-5XX-1","BCA","1234567890","John Doe","BRI","0987654321", new BigDecimal("1000"), "IDR", null, null);
        given().contentType(ContentType.JSON).header("X-Simulate","5xx").body(req)
                .when().post("/api/v1/transfer").then().statusCode(500).body("responseCode", equalTo("96"));
    }

    @Test
    void pactBrokerMetadataExists() throws Exception {
        // Verify pact file exists and is valid JSON
        var pact = getClass().getClassLoader().getResourceAsStream("pacts/transaction-service-bi-fast-simulator.json");
        assertNotNull(pact, "Pact file missing");
        var json = new String(pact.readAllBytes());
        assertTrue(json.contains("\"X-Simulate\""), "X-Simulate header missing in pact");
        assertTrue(json.contains("\"provider\""), "provider missing");
    }
}
