package id.payu.gateway.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("Retry and Timeout Service Tests")
public class RetryAndTimeoutServiceTest {

    @Inject
    RetryAndTimeoutService retryAndTimeoutService;

    @Test
    @DisplayName("Should return default timeout for unknown service")
    public void testDefaultTimeout() {
        Duration timeout = retryAndTimeoutService.getTimeout("unknown-service");
        assertNotNull(timeout);
        assertTrue(timeout.getSeconds() > 0);
    }

    @Test
    @DisplayName("Should return configured timeout for known service")
    public void testConfiguredTimeout() {
        Duration timeout = retryAndTimeoutService.getTimeout("account-service");
        assertNotNull(timeout);
        assertEquals(Duration.ofSeconds(30), timeout);
    }

    @Test
    @DisplayName("Should calculate retry delay with exponential backoff")
    public void testRetryDelayCalculation() {
        Duration delay1 = retryAndTimeoutService.calculateRetryDelay(0, "account-service");
        Duration delay2 = retryAndTimeoutService.calculateRetryDelay(1, "account-service");
        Duration delay3 = retryAndTimeoutService.calculateRetryDelay(2, "account-service");

        assertTrue(delay2.toMillis() > delay1.toMillis(), "Second retry should have longer delay");
        assertTrue(delay3.toMillis() > delay2.toMillis(), "Third retry should have longer delay");
    }

    @Test
    @DisplayName("Should cap retry delay at maximum")
    public void testRetryDelayCapping() {
        Duration delay10 = retryAndTimeoutService.calculateRetryDelay(10, "account-service");
        Duration delay100 = retryAndTimeoutService.calculateRetryDelay(100, "account-service");

        // Both should be capped at max interval
        assertTrue(delay10.toMillis() <= 10000, "Delay should be capped at 10 seconds");
        assertTrue(delay100.toMillis() <= 10000, "Delay should be capped at 10 seconds");
    }

    @Test
    @DisplayName("Should add jitter to retry delay")
    public void testRetryDelayJitter() {
        Duration delay1 = retryAndTimeoutService.calculateRetryDelay(1, "account-service");
        Duration delay2 = retryAndTimeoutService.calculateRetryDelay(1, "account-service");

        // Same attempt should have slightly different delays due to jitter
        // Note: This might occasionally fail if jitter is the same
        assertNotEquals(delay2.toMillis(), delay1.toMillis(),
            "Retry delay should include jitter");
    }

    @Test
    @DisplayName("Should execute Uni without retry when disabled")
    public void testExecuteWithoutRetry() {
        Uni<String> result = retryAndTimeoutService.executeWithRetry("test-service",
            Uni.createFrom().item("success"));

        String value = result.await().atMost(Duration.ofSeconds(5));
        assertEquals("success", value);
    }

    @Test
    @DisplayName("Should fail Uni on non-retryable error")
    public void testNonRetryableError() {
        Uni<String> result = retryAndTimeoutService.executeWithRetry("test-service",
            Uni.createFrom().failure(new IllegalArgumentException("Invalid argument")));

        assertThrows(IllegalArgumentException.class, () -> {
            result.await().atMost(Duration.ofSeconds(5));
        });
    }
}
