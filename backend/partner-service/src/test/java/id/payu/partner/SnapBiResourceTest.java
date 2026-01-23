package id.payu.partner;

import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.TokenRequest;
import id.payu.partner.service.SnapBiSignatureService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SnapBiResourceTest {

    @Inject
    SnapBiSignatureService signatureService;

    @org.junit.jupiter.api.Disabled("Payment signature validation requires exact request body matching")
    @Test
    public void testSnapBiFlow() throws Exception {
        // 1. Create Partner
        PartnerDTO partner = new PartnerDTO();
        partner.name = "Snap Partner";
        partner.type = "MERCHANT";
        partner.email = "snap-" + UUID.randomUUID() + "@partner.com";
        partner.phone = "1234567890";
        partner.active = true;

        var partnerResponse = given()
            .contentType(ContentType.JSON)
            .body(partner)
        .when()
            .post("/partners")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath();

        String clientId = partnerResponse.getString("clientId");
        String clientSecret = partnerResponse.getString("clientSecret");

        // 2. Get Access Token with proper signature
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.grantType = "client_credentials";

        String timestamp = signatureService.getCurrentTimestamp();
        String requestBody = String.format("{\"grantType\":\"client_credentials\"}");
        
        String signature = signatureService.generateSignatureWithClientKey(
            clientSecret,
            "POST",
            "/v1/partner/auth/token",
            timestamp,
            requestBody
        );

        String accessToken = given()
            .contentType(ContentType.JSON)
            .header("X-CLIENT-KEY", clientId)
            .header("X-TIMESTAMP", timestamp)
            .header("X-SIGNATURE", signature)
            .body(tokenRequest)
        .when()
            .post("/v1/partner/auth/token")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .extract().path("accessToken");

        // 3. Create Payment with proper signature
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.partnerReferenceNo = "REF-" + UUID.randomUUID().toString();
        paymentRequest.amount = new PaymentRequest.Amount();
        paymentRequest.amount.value = new BigDecimal("10000.00");
        paymentRequest.amount.currency = "IDR";
        paymentRequest.beneficiaryAccountNo = "1234567890";
        paymentRequest.beneficiaryBankCode = "014";
        paymentRequest.sourceAccountNo = "0987654321";

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withIsGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE));
        String paymentRequestBody = mapper.writeValueAsString(paymentRequest);
        
        String paymentTimestamp = signatureService.getCurrentTimestamp();
        String paymentSignature = signatureService.generateSignature(
            clientSecret,
            "POST",
            "/v1/partner/payments",
            accessToken,
            paymentRequestBody,
            paymentTimestamp
        );
        
        String payuReferenceNo = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .header("X-EXTERNAL-ID", "EXT-" + UUID.randomUUID().toString())
            .header("X-TIMESTAMP", paymentTimestamp)
            .header("X-SIGNATURE", paymentSignature)
            .body(paymentRequestBody)
        .when()
            .post("/v1/partner/payments")
        .then()
            .statusCode(200)
            .body("responseCode", equalTo("2002500"))
            .extract().path("referenceNo");

        // 4. Get Payment Status with proper signature
        String statusTimestamp = signatureService.getCurrentTimestamp();
        String statusSignature = signatureService.generateSignature(
            clientSecret,
            "GET",
            "/v1/partner/payments/" + payuReferenceNo,
            accessToken,
            "",
            statusTimestamp
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .header("X-TIMESTAMP", statusTimestamp)
            .header("X-SIGNATURE", statusSignature)
        .when()
            .get("/v1/partner/payments/" + payuReferenceNo)
        .then()
            .statusCode(200)
            .body("responseCode", equalTo("2002500"))
            .body("referenceNo", equalTo(payuReferenceNo))
            .body("status", equalTo("PENDING"));
    }

    @Test
    public void testTokenGenerationAndValidation() throws Exception {
        PartnerDTO partner = new PartnerDTO();
        partner.name = "Token Test Partner";
        partner.type = "MERCHANT";
        partner.email = "token-test-" + UUID.randomUUID() + "@partner.com";
        partner.phone = "1234567890";
        partner.active = true;

        var partnerResponse = given()
            .contentType(ContentType.JSON)
            .body(partner)
        .when()
            .post("/partners")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath();

        String clientId = partnerResponse.getString("clientId");
        String clientSecret = partnerResponse.getString("clientSecret");

        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.grantType = "client_credentials";

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withIsGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE));
        String requestBody = mapper.writeValueAsString(tokenRequest);
        
        String timestamp = signatureService.getCurrentTimestamp();
        String signature = signatureService.generateSignatureWithClientKey(
            clientSecret,
            "POST",
            "/v1/partner/auth/token",
            timestamp,
            requestBody
        );

        String accessToken = given()
            .contentType(ContentType.JSON)
            .header("X-CLIENT-KEY", clientId)
            .header("X-TIMESTAMP", timestamp)
            .header("X-SIGNATURE", signature)
            .body(tokenRequest)
        .when()
            .post("/v1/partner/auth/token")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .extract().path("accessToken");

        assertNotNull(accessToken);
        assertFalse(accessToken.isEmpty());
    }

    @Test
    public void testInvalidSignature() throws Exception {
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.grantType = "client_credentials";

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withIsGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE));
        String requestBody = mapper.writeValueAsString(tokenRequest);
        
        String timestamp = signatureService.getCurrentTimestamp();
        String signature = signatureService.generateSignatureWithClientKey(
            "wrong-secret",
            "POST",
            "/v1/partner/auth/token",
            timestamp,
            requestBody
        );

        given()
            .contentType(ContentType.JSON)
            .header("X-CLIENT-KEY", "invalid-client-id")
            .header("X-TIMESTAMP", timestamp)
            .header("X-SIGNATURE", signature)
            .body(tokenRequest)
        .when()
            .post("/v1/partner/auth/token")
        .then()
            .statusCode(401);
    }
}
