package id.payu.simulator.qris;

import id.payu.simulator.qris.interfaces.dto.GenerateQrRequest;
import id.payu.simulator.qris.interfaces.dto.PayQrRequest;
import id.payu.simulator.qris.service.QrCodeGenerator;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

@QuarkusTest
class PactVerificationTest {

    @Test void pactGenerateSuccessWithCrc16() {
        GenerateQrRequest req = new GenerateQrRequest("MCH001", new BigDecimal("15000"), null, null, null, null);
        var resp = given().contentType(ContentType.JSON).header("X-Simulate","success").body(req)
                .when().post("/api/v1/generate").then().statusCode(201).body("responseCode", equalTo("00")).extract().asString();
        // CRC16 check on qrContent
        String qr = io.restassured.path.json.JsonPath.from(resp).getString("qrContent");
        assertNotNull(qr);
        assertTrue(QrCodeGenerator.isValidCrc(qr), "QR CRC16 invalid: "+qr);
        assertTrue(qr.contains("6304"), "EMVCo TLV 6304 missing");
    }

    @Test void pactGenerateNotFound(){ GenerateQrRequest req=new GenerateQrRequest("UNKNOWN", new BigDecimal("1000"), null, null, null, null); given().contentType(ContentType.JSON).body(req).when().post("/api/v1/generate").then().statusCode(404).body("responseCode", equalTo("14")); }
    @Test void pactGenerateBlockedViaXSimulate(){ GenerateQrRequest req=new GenerateQrRequest("MCH001", new BigDecimal("1000"), null, null, null, null); given().contentType(ContentType.JSON).header("X-Simulate","blocked").body(req).when().post("/api/v1/generate").then().statusCode(403).body("responseCode", equalTo("62")); }
    @Test void pactGenerateRateLimitViaXSimulate(){ GenerateQrRequest req=new GenerateQrRequest("MCH001", new BigDecimal("1000"), null, null, null, null); given().contentType(ContentType.JSON).header("X-Simulate","rate-limit").body(req).when().post("/api/v1/generate").then().statusCode(429); }
    @Test void pactPaySuccess(){ 
        GenerateQrRequest gen=new GenerateQrRequest("MCH001", new BigDecimal("10000"), null, null, null, null);
        String qrId = given().contentType(ContentType.JSON).header("X-Simulate","success").body(gen).when().post("/api/v1/generate").then().statusCode(201).extract().path("qrId");
        PayQrRequest pay=new PayQrRequest(qrId,"John Doe","1234567890","BCA", null, null, false);
        given().contentType(ContentType.JSON).header("X-Simulate","success").header("X-Idempotency-Key","QR-PAY-001").body(pay).when().post("/api/v1/pay").then().statusCode(200).body("responseCode", equalTo("00"));
    }
    @Test void pactPayNotFound(){ PayQrRequest pay=new PayQrRequest("QR-NOTFOUND","John","123","BCA", null, null, false); given().contentType(ContentType.JSON).body(pay).when().post("/api/v1/pay").then().statusCode(404).body("responseCode", equalTo("14")); }
    @Test void pactQrCrc16Unit(){
        String c = new QrCodeGenerator().generateQrisContent("MCH001","Test Merchant", new BigDecimal("10000"), "REF123");
        assertTrue(QrCodeGenerator.isValidCrc(c), "Generated QR must have valid CRC16");
        // tamper should fail
        String tampered = c.substring(0,c.length()-1)+"0";
        assertFalse(QrCodeGenerator.isValidCrc(tampered));
    }
    @Test void pactFileContainsXSimulateAndCrc() throws Exception { var s=getClass().getClassLoader().getResourceAsStream("pacts/wallet-service-qris-simulator.json"); assertNotNull(s); var j=new String(s.readAllBytes()); assertTrue(j.contains("X-Simulate")); assertTrue(j.contains("qrContent")); assertTrue(j.contains("6304")); }
}
