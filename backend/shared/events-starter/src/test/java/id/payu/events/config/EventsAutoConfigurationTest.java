package id.payu.events.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventsAutoConfiguration")
class EventsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventsAutoConfiguration.class));

    @Nested
    @DisplayName("Default configuration")
    class DefaultConfig {

        @Test
        @DisplayName("should auto-configure when payu.events.enabled is not set (matchIfMissing)")
        void enabledByDefault() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(EventsAutoConfiguration.CloudEventsInitializer.class);
                assertThat(context).hasBean("cloudEventsObjectMapper");
            });
        }

        @Test
        @DisplayName("should create cloudEventsObjectMapper bean")
        void objectMapperBean() {
            contextRunner.run(context -> {
                assertThat(context).hasBean("cloudEventsObjectMapper");
                var mapper = context.getBean("cloudEventsObjectMapper", com.fasterxml.jackson.databind.ObjectMapper.class);
                assertThat(mapper).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("Disabled configuration")
    class DisabledConfig {

        @Test
        @DisplayName("should not auto-configure when payu.events.enabled=false")
        void disabledExplicitly() {
            contextRunner
                    .withPropertyValues("payu.events.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(EventsAutoConfiguration.CloudEventsInitializer.class);
                    });
        }
    }

    @Nested
    @DisplayName("EventsProperties")
    class PropertiesTest {

        @Test
        @DisplayName("should bind properties from application config")
        void bindProperties() {
            contextRunner
                    .withPropertyValues(
                            "payu.events.enabled=true",
                            "payu.events.kafka.enabled=true",
                            "payu.events.kafka.default-topic=payu.custom.events",
                            "payu.events.kafka.bootstrap-servers=kafka:29092"
                    )
                    .run(context -> {
                        var props = context.getBean(EventsAutoConfiguration.EventsProperties.class);
                        assertThat(props.isEnabled()).isTrue();
                        assertThat(props.getKafka().isEnabled()).isTrue();
                        assertThat(props.getKafka().getDefaultTopic()).isEqualTo("payu.custom.events");
                        assertThat(props.getKafka().getBootstrapServers()).isEqualTo("kafka:29092");
                    });
        }

        @Test
        @DisplayName("should have sensible defaults")
        void defaults() {
            var props = new EventsAutoConfiguration.EventsProperties();
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getKafka().isEnabled()).isFalse();
            assertThat(props.getKafka().getDefaultTopic()).isEqualTo("payu.events");
            assertThat(props.getKafka().getBootstrapServers()).isEqualTo("localhost:9092");
        }
    }
}
