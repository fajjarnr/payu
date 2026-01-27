package id.payu.partner;

import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.RefundRequest;
import id.payu.partner.dto.snap.TokenRequest;
import id.payu.partner.service.SnapBiPaymentService;
import id.payu.partner.service.SnapBiSignatureService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Disabled("Integration tests require Kafka/Docker - disabled when Docker not available")
public class TokoBapakIntegrationTest {

    @Inject
    SnapBiSignatureService signatureService;

    @Inject
    SnapBiPaymentService paymentService;

    @Test
    public void testTokoBapakFullFlowWithRefund() throws Exception {
        String partnerName = "TokoBapak-" + UUID.randomUUID().toString().substring(0, 8);

        PartnerDTO partner = new PartnerDTO();
        partner.name = partnerName;
        partner.type = "MERCHANT";
        partner.email = "tokobapak-" + UUID.randomUUID() + "@tokobapak.com";
        partner.phone = "08123456789";
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

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.partnerReferenceNo = "TB-REF-" + UUID.randomUUID().toString();
        paymentRequest.amount = new PaymentRequest.Amount();
        paymentRequest.amount.value = new BigDecimal("150000.00");
        paymentRequest.amount.currency = "IDR";
        paymentRequest.beneficiaryAccountNo = "8888777766665555";
        paymentRequest.beneficiaryBankCode = "014";
        paymentRequest.sourceAccountNo = "0000111122223333";

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
            .header("X-EXTERNAL-ID", "TB-EXT-" + UUID.randomUUID().toString())
            .header("X-TIMESTAMP", paymentTimestamp)
            .header("X-SIGNATURE", paymentSignature)
            .body(paymentRequestBody)
        .when()
            .post("/v1/partner/payments")
        .then()
            .statusCode(200)
            .body("responseCode", equalTo("2002500"))
            .body("referenceNo", notNullValue())
            .extract().path("referenceNo");

        assertNotNull(payuReferenceNo);

        paymentService.updatePaymentStatus(payuReferenceNo, "COMPLETED");

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.partnerRefundNo = "TB-REFUND-" + UUID.randomUUID().toString();
        refundRequest.amount = new RefundRequest.Amount();
        refundRequest.amount.value = new BigDecimal("150000.00");
        refundRequest.amount.currency = "IDR";
        refundRequest.reason = "Customer request - item out of stock";

        String refundRequestBody = mapper.writeValueAsString(refundRequest);

        String refundTimestamp = signatureService.getCurrentTimestamp();
        String refundSignature = signatureService.generateSignature(
            clientSecret,
            "POST",
            "/v1/partner/payments/" + payuReferenceNo + "/refund",
            accessToken,
            refundRequestBody,
            refundTimestamp
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .header("X-TIMESTAMP", refundTimestamp)
            .header("X-SIGNATURE", refundSignature)
            .body(refundRequestBody)
        .when()
            .post("/v1/partner/payments/" + payuReferenceNo + "/refund")
        .then()
            .statusCode(200)
            .body("responseCode", equalTo("2002500"))
            .body("payuRefundNo", notNullValue())
            .body("referenceNo", equalTo(payuReferenceNo))
            .body("refundStatus", equalTo("COMPLETED"));
    }

    @Test
    public void testRefundNonExistentPayment() throws Exception {
        PartnerDTO partner = new PartnerDTO();
        partner.name = "Refund Test Partner";
        partner.type = "MERCHANT";
        partner.email = "refund-test-" + UUID.randomUUID() + "@partner.com";
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

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.partnerRefundNo = "REF-TEST-" + UUID.randomUUID().toString();
        refundRequest.amount = new RefundRequest.Amount();
        refundRequest.amount.value = new BigDecimal("10000.00");
        refundRequest.amount.currency = "IDR";
        refundRequest.reason = "Test reason";

        String refundRequestBody = mapper.writeValueAsString(refundRequest);

        String refundTimestamp = signatureService.getCurrentTimestamp();
        String refundSignature = signatureService.generateSignature(
            clientSecret,
            "POST",
            "/v1/partner/payments/NONEXISTENT/refund",
            accessToken,
            refundRequestBody,
            refundTimestamp
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .header("X-TIMESTAMP", refundTimestamp)
            .header("X-SIGNATURE", refundSignature)
            .body(refundRequestBody)
        .when()
            .post("/v1/partner/payments/NONEXISTENT/refund")
        .then()
            .statusCode(200)
            .body("responseCode", equalTo("4042500"));
    }

    @Test
    public void testRefundPendingPayment() throws Exception {
        PartnerDTO partner = new PartnerDTO();
        partner.name = "Refund Pending Test Partner";
        partner.type = "MERCHANT";
        partner.email = "refund-pending-" + UUID.randomUUID() + "@partner.com";
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

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.partnerReferenceNo = "REF-TB-" + UUID.randomUUID().toString();
        paymentRequest.amount = new PaymentRequest.Amount();
        paymentRequest.amount.value = new BigDecimal("5000.00");
        paymentRequest.amount.currency = "IDR";
        paymentRequest.beneficiaryAccountNo = "9999888877776666";
        paymentRequest.beneficiaryBankCode = "014";
        paymentRequest.sourceAccountNo = "1111222233334444";

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
            .header("X-EXTERNAL-ID", "EXT-REF-TB-" + UUID.randomUUID().toString())
            .header("X-TIMESTAMP", paymentTimestamp)
            .header("X-SIGNATURE", paymentSignature)
            .body(paymentRequestBody)
        .when()
            .post("/v1/partner/payments")
        .then()
            .statusCode(200)
            .body("responseCode", equalTo("2002500"))
            .extract().path("referenceNo");

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.partnerRefundNo = "REF-TB-" + UUID.randomUUID().toString();
        refundRequest.amount = new RefundRequest.Amount();
        refundRequest.amount.value = new BigDecimal("5000.00");
        refundRequest.amount.currency = "IDR";
        refundRequest.reason = "Try to refund pending payment";

        String refundRequestBody = mapper.writeValueAsString(refundRequest);

        String refundTimestamp = signatureService.getCurrentTimestamp();
        String refundSignature = signatureService.generateSignature(
            clientSecret,
            "POST",
            "/v1/partner/payments/" + payuReferenceNo + "/refund",
            accessToken,
            refundRequestBody,
            refundTimestamp
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .header("X-TIMESTAMP", refundTimestamp)
            .header("X-SIGNATURE", refundSignature)
            .body(refundRequestBody)
        .when()
            .post("/v1/partner/payments/" + payuReferenceNo + "/refund")
        .then()
            .statusCode(200)
            .body("responseCode", equalTo("4002502"));
    }
}
