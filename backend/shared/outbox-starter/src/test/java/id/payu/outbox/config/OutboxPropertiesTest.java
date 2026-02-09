package id.payu.outbox.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OutboxProperties}.
 * Validates default values and configuration properties.
 */
@DisplayName("OutboxProperties")
class OutboxPropertiesTest {

    @Nested
    @DisplayName("Default values")
    class DefaultValueTests {

        @Test
        @DisplayName("should have outbox enabled by default")
        void shouldBeEnabledByDefault() {
            OutboxProperties props = new OutboxProperties();
            assertThat(props.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have default publisher properties")
        void shouldHaveDefaultPublisherProperties() {
            OutboxProperties props = new OutboxProperties();
            OutboxProperties.PublisherProperties publisher = props.getPublisher();

            assertThat(publisher).isNotNull();
            assertThat(publisher.getBatchSize()).isEqualTo(100);
            assertThat(publisher.getPollIntervalMs()).isEqualTo(1000);
            assertThat(publisher.getMaxRetries()).isEqualTo(3);
            assertThat(publisher.getDefaultTopic()).isEqualTo("outbox.events");
            assertThat(publisher.getLockTimeoutMs()).isEqualTo(10000);
            assertThat(publisher.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have default cleanup properties")
        void shouldHaveDefaultCleanupProperties() {
            OutboxProperties props = new OutboxProperties();
            OutboxProperties.CleanupProperties cleanup = props.getCleanup();

            assertThat(cleanup).isNotNull();
            assertThat(cleanup.isEnabled()).isTrue();
            assertThat(cleanup.getRetentionDays()).isEqualTo(30);
            assertThat(cleanup.getFailedRetentionDays()).isEqualTo(7);
            assertThat(cleanup.getCron()).isEqualTo("0 0 2 * * *");
        }
    }

    @Nested
    @DisplayName("Custom values")
    class CustomValueTests {

        @Test
        @DisplayName("should allow overriding publisher properties")
        void shouldAllowOverridingPublisherProperties() {
            OutboxProperties props = new OutboxProperties();
            OutboxProperties.PublisherProperties publisher = props.getPublisher();

            publisher.setBatchSize(500);
            publisher.setMaxRetries(5);
            publisher.setDefaultTopic("custom.events");
            publisher.setPollIntervalMs(5000);

            assertThat(publisher.getBatchSize()).isEqualTo(500);
            assertThat(publisher.getMaxRetries()).isEqualTo(5);
            assertThat(publisher.getDefaultTopic()).isEqualTo("custom.events");
            assertThat(publisher.getPollIntervalMs()).isEqualTo(5000);
        }

        @Test
        @DisplayName("should allow overriding cleanup properties")
        void shouldAllowOverridingCleanupProperties() {
            OutboxProperties props = new OutboxProperties();
            OutboxProperties.CleanupProperties cleanup = props.getCleanup();

            cleanup.setRetentionDays(90);
            cleanup.setFailedRetentionDays(14);
            cleanup.setCron("0 0 3 * * *");
            cleanup.setEnabled(false);

            assertThat(cleanup.getRetentionDays()).isEqualTo(90);
            assertThat(cleanup.getFailedRetentionDays()).isEqualTo(14);
            assertThat(cleanup.getCron()).isEqualTo("0 0 3 * * *");
            assertThat(cleanup.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should allow disabling outbox entirely")
        void shouldAllowDisabling() {
            OutboxProperties props = new OutboxProperties();
            props.setEnabled(false);

            assertThat(props.isEnabled()).isFalse();
        }
    }
}
