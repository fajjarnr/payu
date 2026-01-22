package id.payu.partner;

import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.dto.snap.PaymentRequest;
import id.payu.partner.dto.snap.TokenRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class SnapBiResourceTest {

    @Test
    public void testSnapBiFlow() {
        // 1. Create Partner
        PartnerDTO partner = new PartnerDTO();
        partner.name = "Snap Partner";
        partner.type = "MERCHANT";
        partner.email = "snap@partner.com";
        partner.phone = "1234567890";
        partner.active = true;

        String clientId = given()
            .contentType(ContentType.JSON)
            .body(partner)
        .when()
            .post("/partners")
        .then()
            .statusCode(201)
            .extract().path("clientId");

        // 2. Get Access Token
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.grantType = "client_credentials";

        String accessToken = given()
            .contentType(ContentType.JSON)
            .header("X-CLIENT-KEY", clientId)
            .header("X-TIMESTAMP", "2023-10-01T12:00:00Z")
            .header("X-SIGNATURE", "dummy-signature")
            .body(tokenRequest)
        .when()
            .post("/v1/partner/auth/token")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .extract().path("accessToken");

        // 3. Create Payment
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.partnerReferenceNo = "REF-" + UUID.randomUUID().toString();
        paymentRequest.amount = new PaymentRequest.Amount();
        paymentRequest.amount.value = new BigDecimal("10000.00");
        paymentRequest.amount.currency = "IDR";
        
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .header("X-EXTERNAL-ID", "EXT-" + UUID.randomUUID().toString())
            .body(paymentRequest)
        .when()
            .post("/v1/partner/payments")
        .then()
            .statusCode(200)
            .body("responseCode", equalTo("2002500"));
    }
}
