package id.payu.simulator.biller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PactVerificationTest {

    @Test void pactInquirySuccess(){ given().contentType(ContentType.JSON).body(Map.of("billerCode","PLN","customerNumber","PLN-001234567890")).when().post("/api/v1/biller/inquiry").then().statusCode(200).body("responseCode", equalTo("00")); }
    @Test void pactInquiryNotFound(){ given().contentType(ContentType.JSON).body(Map.of("billerCode","PLN","customerNumber","PLN-000000000000")).when().post("/api/v1/biller/inquiry").then().statusCode(400).body("responseCode", equalTo("14")); }
    @Test void pactInquiryBlockedViaXSimulate(){ given().contentType(ContentType.JSON).header("X-Simulate","blocked").body(Map.of("billerCode","PLN","customerNumber","PLN-001234567890")).when().post("/api/v1/biller/inquiry").then().statusCode(403); }
    @Test void pactInquiryRateLimitViaXSimulate(){ given().contentType(ContentType.JSON).header("X-Simulate","rate-limit").body(Map.of("billerCode","PLN","customerNumber","PLN-001234567890")).when().post("/api/v1/biller/inquiry").then().statusCode(429); }
    @Test void pactPaySuccess(){ given().contentType(ContentType.JSON).header("X-Idempotency-Key","BILL-001").body(Map.of("billerCode","PDAM","customerNumber","PDAM-001234567890","amount",89000,"referenceNumber","BILL-PACT-001")).when().post("/api/v1/biller/pay").then().statusCode(200).body("responseCode", equalTo("00")); }
    @Test void pactPayDuplicate(){ 
        given().contentType(ContentType.JSON).body(Map.of("billerCode","PDAM","customerNumber","PDAM-001234567890","amount",1000,"referenceNumber","BILL-PACT-DUP-001")).when().post("/api/v1/biller/pay").then().statusCode(200);
        given().contentType(ContentType.JSON).body(Map.of("billerCode","PDAM","customerNumber","PDAM-001234567890","amount",1000,"referenceNumber","BILL-PACT-DUP-001")).when().post("/api/v1/biller/pay").then().statusCode(409).body("responseCode", equalTo("94"));
    }
    @Test void pactPayTimeoutViaXSimulate(){ given().contentType(ContentType.JSON).header("X-Simulate","timeout").body(Map.of("billerCode","PLN","customerNumber","PLN-001234567890","amount",1000,"referenceNumber","BILL-TIMEOUT-1")).when().post("/api/v1/biller/pay").then().statusCode(500); }
    @Test void pactFileExists() throws Exception { var s=getClass().getClassLoader().getResourceAsStream("pacts/billing-service-biller-simulator.json"); assertNotNull(s); var j=new String(s.readAllBytes()); assertTrue(j.contains("X-Simulate")); }
}
