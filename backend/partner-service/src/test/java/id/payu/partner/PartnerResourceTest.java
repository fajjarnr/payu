package id.payu.partner;

import id.payu.partner.dto.PartnerDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PartnerResourceTest {

    @Test
    public void testCreateAndGetPartner() {
        PartnerDTO partner = new PartnerDTO();
        partner.name = "Test Partner";
        partner.type = "MERCHANT";
        partner.email = "test@partner.com";
        partner.phone = "1234567890";
        partner.active = true;

        // Create
        given()
            .contentType(ContentType.JSON)
            .body(partner)
        .when()
            .post("/partners")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Test Partner"));

        // Get
        given()
        .when()
            .get("/partners")
        .then()
            .statusCode(200)
            .body("$.size()", greaterThan(0));
    }
}
