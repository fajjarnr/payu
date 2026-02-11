package id.payu.outbox;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Test configuration for outbox-starter integration tests.
 * <p>
 * Provides a minimal Spring Boot application context for testing the outbox
 * pattern without requiring a full service infrastructure.
 * <p>
 * Features:
 * <ul>
 *   <li>H2 in-memory database for fast test execution</li>
 *   <li>Kafka auto-configuration excluded (mocked)</li>
 *   <li>Component scanning for outbox package</li>
 * </ul>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@SpringBootApplication(exclude = {
        KafkaAutoConfiguration.class
})
@ComponentScan(basePackages = "id.payu.outbox", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                // Exclude the scheduler and publisher to prevent background processing during tests
                id.payu.outbox.scheduler.OutboxCleanupScheduler.class
        })
})
public class TestConfig {
    // This class serves as a minimal Spring Boot application for testing
}
