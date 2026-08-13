package id.payu.events.cloudevents;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CloudEventBuilder")
class CloudEventBuilderTest {

    @Nested
    @DisplayName("Required fields validation")
    class RequiredFields {

        @Test
        @DisplayName("should throw when source is not set")
        void missingSource() {
            CloudEventBuilder<String> builder = new CloudEventBuilder<String>()
                    .type("test.event");

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Source");
        }

        @Test
        @DisplayName("should throw when type is not set")
        void missingType() {
            CloudEventBuilder<String> builder = new CloudEventBuilder<String>()
                    .source(URI.create("/test"));

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Type");
        }

        @Test
        @DisplayName("should throw when type is blank")
        void blankType() {
            CloudEventBuilder<String> builder = new CloudEventBuilder<String>()
                    .source(URI.create("/test"))
                    .type("   ");

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Type");
        }
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApi {

        @Test
        @DisplayName("should build envelope with all fields")
        void allFields() {
            UUID id = UUID.randomUUID();
            OffsetDateTime time = OffsetDateTime.parse("2026-01-15T10:30:00+07:00");

            CloudEventEnvelope<String> envelope = new CloudEventBuilder<String>()
                    .id(id)
                    .source(URI.create("/services/wallet-service"))
                    .type("id.payu.wallet.credited")
                    .dataContentType("application/json")
                    .time(time)
                    .subject("wallet-456")
                    .data("credit-event")
                    .payuTraceContext("trace-001")
                    .payuCorrelationId("corr-001")
                    .build();

            assertThat(envelope.getId()).isEqualTo(id);
            assertThat(envelope.getSource()).isEqualTo(URI.create("/services/wallet-service"));
            assertThat(envelope.getType()).isEqualTo("id.payu.wallet.credited");
            assertThat(envelope.getTime()).isEqualTo(time);
            assertThat(envelope.getSubject()).isEqualTo("wallet-456");
            assertThat(envelope.getData()).isEqualTo("credit-event");
            assertThat(envelope.getPayuTraceContext()).isEqualTo("trace-001");
            assertThat(envelope.getPayuCorrelationId()).isEqualTo("corr-001");
        }

        @Test
        @DisplayName("should accept string source URI")
        void stringSource() {
            CloudEventEnvelope<String> envelope = new CloudEventBuilder<String>()
                    .source("/services/test")
                    .type("test.event")
                    .build();

            assertThat(envelope.getSource()).isEqualTo(URI.create("/services/test"));
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("forService should set source to /services/{name}")
        void forService() {
            CloudEventEnvelope<String> envelope = CloudEventBuilder.<String>forService("account-service")
                    .type("id.payu.account.created")
                    .data("new-account")
                    .build();

            assertThat(envelope.getSource()).isEqualTo(URI.create("/services/account-service"));
            assertThat(envelope.getType()).isEqualTo("id.payu.account.created");
        }

        @Test
        @DisplayName("forDomain should set source to /domains/{domain}")
        void forDomain() {
            CloudEventEnvelope<String> envelope = CloudEventBuilder.<String>forDomain("transaction")
                    .type("id.payu.transaction.completed")
                    .data("txn-data")
                    .build();

            assertThat(envelope.getSource()).isEqualTo(URI.create("/domains/transaction"));
        }
    }

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("should generate unique IDs for each build")
        void uniqueIds() {
            CloudEventEnvelope<String> envelope1 = new CloudEventBuilder<String>()
                    .source("/test").type("test.event").build();
            CloudEventEnvelope<String> envelope2 = new CloudEventBuilder<String>()
                    .source("/test").type("test.event").build();

            assertThat(envelope1.getId()).isNotEqualTo(envelope2.getId());
        }

        @Test
        @DisplayName("should set specVersion to 1.0.2")
        void defaultSpecVersion() {
            CloudEventEnvelope<String> envelope = new CloudEventBuilder<String>()
                    .source("/test").type("test.event").build();

            assertThat(envelope.getSpecVersion()).isEqualTo("1.0.2");
        }

        @Test
        @DisplayName("should allow overriding specVersion")
        void overrideSpecVersion() {
            CloudEventEnvelope<String> envelope = new CloudEventBuilder<String>()
                    .specVersion("1.1")
                    .source("/test")
                    .type("test.event")
                    .build();

            assertThat(envelope.getSpecVersion()).isEqualTo("1.1");
        }
    }
}
