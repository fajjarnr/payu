package id.payu.simulator.va;

import id.payu.simulator.va.interfaces.dto.VaInquiryRequest;
import id.payu.simulator.va.interfaces.dto.VaPaymentRequest;
import id.payu.simulator.va.interfaces.dto.VaRegistrationRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.transaction.Transactional;
import id.payu.simulator.va.entity.VirtualAccount;
import java.math.BigDecimal;
import java.time.Instant;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PactVerificationTest {

    @BeforeEach
    @Transactional
    void clean(){ VirtualAccount.deleteAll(); }

    void registerVA(){
        VaRegistrationRequest r = new VaRegistrationRequest("123456789012","BCA","Bank Central Asia","partner-123", new BigDecimal("100000.00"), "IDR", Instant.now().plusSeconds(3600), "http://localhost/callback","ext-123","John Doe","Test");
        given().contentType(ContentType.JSON).body(r).when().post("/api/v1/va/register").then().statusCode(201);
    }

    @Test void pactInquirySuccess(){ registerVA(); given().contentType(ContentType.JSON).body(new VaInquiryRequest("123456789012","BCA",null,null)).when().post("/api/v1/va/inquiry").then().statusCode(200).body("responseCode", equalTo("00")); }
    @Test void pactInquiryNotFound(){ given().contentType(ContentType.JSON).body(new VaInquiryRequest("999999999999","BCA",null,null)).when().post("/api/v1/va/inquiry").then().statusCode(404).body("responseCode", equalTo("14")); }
    @Test void pactInquiryBlockedViaXSimulate(){ registerVA(); given().contentType(ContentType.JSON).header("X-Simulate","blocked").body(new VaInquiryRequest("123456789012","BCA",null,null)).when().post("/api/v1/va/inquiry").then().statusCode(403); }
    @Test void pactInquiryRateLimitViaXSimulate(){ registerVA(); given().contentType(ContentType.JSON).header("X-Simulate","rate-limit").body(new VaInquiryRequest("123456789012","BCA",null,null)).when().post("/api/v1/va/inquiry").then().statusCode(429); }
    @Test void pactPaySuccessWithIdempotency(){
        registerVA();
        VaPaymentRequest p = new VaPaymentRequest("123456789012","BCA", new BigDecimal("100000.00"), "IDR", "9876543210","Jane Doe","ATM","REF123");
        given().contentType(ContentType.JSON).header("X-Idempotency-Key","VA-PAY-001").header("X-Simulate","success").body(p).when().post("/api/v1/va/pay").then().statusCode(200).body("responseCode", equalTo("00")).body("vaNumber", equalTo("123456789012"));
    }
    @Test void pactPayAlreadyPaid(){
        registerVA();
        VaPaymentRequest p = new VaPaymentRequest("123456789012","BCA", new BigDecimal("100000.00"), "IDR", "9876543210","Jane Doe","ATM","REF123");
        given().contentType(ContentType.JSON).body(p).when().post("/api/v1/va/pay").then().statusCode(200);
        given().contentType(ContentType.JSON).body(p).when().post("/api/v1/va/pay").then().statusCode(409).body("responseCode", equalTo("94"));
    }
    @Test void pactPactFileExists() throws Exception { var s=getClass().getClassLoader().getResourceAsStream("pacts/payment-service-va-simulator.json"); assertNotNull(s); var j=new String(s.readAllBytes()); assertTrue(j.contains("X-Simulate")); }
}
