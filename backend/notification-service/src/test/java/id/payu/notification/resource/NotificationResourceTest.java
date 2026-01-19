package id.payu.notification.resource;

import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.dto.SendNotificationRequest;
import id.payu.notification.sender.EmailSender;
import id.payu.notification.sender.PushSender;
import id.payu.notification.sender.SmsSender;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@DisplayName("Notification Resource Tests")
class NotificationResourceTest {

    @InjectMock
    EmailSender emailSender;

    @InjectMock
    SmsSender smsSender;

    @InjectMock
    PushSender pushSender;

    @Test
    @DisplayName("POST /api/v1/notifications - should send email notification")
    void shouldSendEmailNotification() {
        when(emailSender.send(any())).thenReturn(true);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": "user-123",
                            "channel": "EMAIL",
                            "recipient": "user@example.com",
                            "title": "Test Notification",
                            "body": "This is a test notification"
                        }
                        """)
                .when()
                .post("/api/v1/notifications")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("userId", equalTo("user-123"))
                .body("channel", equalTo("EMAIL"))
                .body("status", equalTo("SENT"));
    }

    @Test
    @DisplayName("POST /api/v1/notifications - should send push notification")
    void shouldSendPushNotification() {
        when(pushSender.send(any())).thenReturn(true);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": "user-123",
                            "channel": "PUSH",
                            "recipient": "device-token-xyz",
                            "title": "New Transaction",
                            "body": "You received Rp 100.000"
                        }
                        """)
                .when()
                .post("/api/v1/notifications")
                .then()
                .statusCode(201)
                .body("channel", equalTo("PUSH"))
                .body("status", equalTo("SENT"));
    }

    @Test
    @DisplayName("POST /api/v1/notifications - should validate request")
    void shouldValidateRequest() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": "",
                            "channel": "EMAIL"
                        }
                        """)
                .when()
                .post("/api/v1/notifications")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /api/v1/notifications/{id} - should return 404 for non-existent")
    void shouldReturn404ForNonExistent() {
        given()
                .when()
                .get("/api/v1/notifications/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GET /api/v1/notifications/user/{userId} - should return empty list")
    void shouldReturnEmptyListForNewUser() {
        given()
                .when()
                .get("/api/v1/notifications/user/unknown-user")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }
}
