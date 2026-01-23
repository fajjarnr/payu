package id.payu.billing.resource;

import id.payu.billing.client.WalletClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@DisplayName("Top-up Resource Tests")
class TopUpResourceTest {

    @InjectMock
    @RestClient
    WalletClient walletClient;

    @Test
    @DisplayName("POST /api/v1/topup - should create GoPay top-up")
    void shouldCreateGoPayTopUp() {
        Mockito.when(walletClient.reserveBalance(anyString(), any()))
            .thenReturn(new WalletClient.ReserveResponse("res-123", "ACC-001", "ref-123", "RESERVED"));

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY",
                    "walletNumber": "08123456789",
                    "amount": 100000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(201)
            .body("referenceNumber", startsWith("BILL"))
            .body("provider", equalTo("GOPAY"))
            .body("walletNumber", equalTo("08123456789"))
            .body("amount", equalTo(100000))
            .body("adminFee", equalTo(1000))
            .body("totalAmount", equalTo(101000))
            .body("status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create OVO top-up")
    void shouldCreateOVOTopUp() {
        Mockito.when(walletClient.reserveBalance(anyString(), any()))
            .thenReturn(new WalletClient.ReserveResponse("res-456", "ACC-002", "ref-456", "RESERVED"));

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-002",
                    "provider": "OVO",
                    "walletNumber": "08987654321",
                    "amount": 50000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(201)
            .body("provider", equalTo("OVO"))
            .body("walletNumber", equalTo("08987654321"))
            .body("amount", equalTo(50000))
            .body("adminFee", equalTo(1000))
            .body("status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create DANA top-up")
    void shouldCreateDNATopUp() {
        Mockito.when(walletClient.reserveBalance(anyString(), any()))
            .thenReturn(new WalletClient.ReserveResponse("res-789", "ACC-003", "ref-789", "RESERVED"));

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-003",
                    "provider": "DANA",
                    "walletNumber": "08555555555",
                    "amount": 300000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(201)
            .body("provider", equalTo("DANA"))
            .body("walletNumber", equalTo("08555555555"))
            .body("amount", equalTo(300000))
            .body("adminFee", equalTo(1500))
            .body("totalAmount", equalTo(301500))
            .body("status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create LinkAja top-up")
    void shouldCreateLinkAjaTopUp() {
        Mockito.when(walletClient.reserveBalance(anyString(), any()))
            .thenReturn(new WalletClient.ReserveResponse("res-999", "ACC-004", "ref-999", "RESERVED"));

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-004",
                    "provider": "LINKAJA",
                    "walletNumber": "08777777777",
                    "amount": 1000000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(201)
            .body("provider", equalTo("LINKAJA"))
            .body("walletNumber", equalTo("08777777777"))
            .body("amount", equalTo(1000000))
            .body("adminFee", equalTo(2000))
            .body("totalAmount", equalTo(1002000))
            .body("status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should fail for unknown provider")
    void shouldFailForUnknownProvider() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "UNKNOWN",
                    "walletNumber": "08123456789",
                    "amount": 100000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate minimum amount")
    void shouldValidateMinimumAmount() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY",
                    "walletNumber": "08123456789",
                    "amount": 5000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate maximum amount")
    void shouldValidateMaximumAmount() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY",
                    "walletNumber": "08123456789",
                    "amount": 5000000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate wallet number length")
    void shouldValidateWalletNumberLength() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY",
                    "walletNumber": "081234567",
                    "amount": 100000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate required fields")
    void shouldValidateRequiredFields() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "",
                    "provider": "GOPAY"
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("GET /api/v1/topup/providers - should return available providers")
    void shouldReturnAvailableProviders() {
        given()
            .when()
            .get("/api/v1/topup/providers")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(4))
            .body("[0].code", equalTo("GOPAY"))
            .body("[0].name", equalTo("GoPay"))
            .body("[1].code", equalTo("OVO"))
            .body("[1].name", equalTo("OVO"))
            .body("[2].code", equalTo("DANA"))
            .body("[2].name", equalTo("DANA"))
            .body("[3].code", equalTo("LINKAJA"))
            .body("[3].name", equalTo("LinkAja"));
    }

    @Test
    @DisplayName("GET /api/v1/topup/{id} - should return 404 for non-existent top-up")
    void shouldReturn404ForNonExistentTopUp() {
        given()
            .when()
            .get("/api/v1/topup/00000000-0000-0000-0000-000000000000")
            .then()
            .statusCode(404);
    }
}