package id.payu.simulator.dukcapil;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PactVerificationTest {

    @Test void pactVerifySuccess(){ given().contentType(ContentType.JSON).header("X-Simulate","success").body(Map.of("nik","3201234567890001","fullName","JOHN DOE")).when().post("/api/v1/verify").then().statusCode(200).body("responseCode", equalTo("00")); }
    @Test void pactVerifyNotFound(){ given().contentType(ContentType.JSON).body(Map.of("nik","9999999999999999","fullName","NONEXISTENT")).when().post("/api/v1/verify").then().statusCode(404).body("responseCode", equalTo("14")); }
    @Test void pactVerifyBlockedViaXSimulate(){ given().contentType(ContentType.JSON).header("X-Simulate","blocked").body(Map.of("nik","3201234567890001","fullName","JOHN DOE")).when().post("/api/v1/verify").then().statusCode(403); }
    @Test void pactVerifyRateLimitViaXSimulate(){ given().contentType(ContentType.JSON).header("X-Simulate","rate-limit").body(Map.of("nik","3201234567890001","fullName","JOHN DOE")).when().post("/api/v1/verify").then().statusCode(429); }
    @Test void pactMatchSuccess(){ given().contentType(ContentType.JSON).body(Map.of("nik","3201234567890001","ktpPhotoBase64","abc","selfiePhotoBase64","def","livenessCheck",true)).when().post("/api/v1/match-photo").then().statusCode(200); }
    @Test void pactGetCitizen(){ given().when().get("/api/v1/nik/3201234567890001").then().statusCode(200).body("responseCode", equalTo("00")); }
    @Test void pactFileExists() throws Exception { var s=getClass().getClassLoader().getResourceAsStream("pacts/kyc-service-dukcapil-simulator.json"); assertNotNull(s); var j=new String(s.readAllBytes()); assertTrue(j.contains("X-Simulate")); }
}
