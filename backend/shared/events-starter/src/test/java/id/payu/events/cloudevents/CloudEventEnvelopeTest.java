package id.payu.events.cloudevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CloudEventEnvelope")
class CloudEventEnvelopeTest {

    @Nested
    @DisplayName("Builder defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("should set specVersion to 1.0 by default")
        void defaultSpecVersion() {
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/test"))
                    .type("test.event")
                    .build();

            assertThat(envelope.getSpecVersion()).isEqualTo("1.0");
        }

        @Test
        @DisplayName("should generate random UUID id by default")
        void defaultId() {
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/test"))
                    .type("test.event")
                    .build();

            assertThat(envelope.getId()).isNotNull();
        }

        @Test
        @DisplayName("should set dataContentType to application/json by default")
        void defaultDataContentType() {
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/test"))
                    .type("test.event")
                    .build();

            assertThat(envelope.getDataContentType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("should set time to approximately now by default")
        void defaultTime() {
            OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/test"))
                    .type("test.event")
                    .build();
            OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

            assertThat(envelope.getTime()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("Builder with custom values")
    class BuilderCustomValues {

        @Test
        @DisplayName("should accept custom data payload")
        void customData() {
            Map<String, Object> payload = Map.of("amount", 100000, "currency", "IDR");

            CloudEventEnvelope<Map<String, Object>> envelope = CloudEventEnvelope.<Map<String, Object>>builder()
                    .source(URI.create("/services/wallet-service"))
                    .type("id.payu.wallet.credited")
                    .data(payload)
                    .build();

            assertThat(envelope.getData()).isEqualTo(payload);
            assertThat(envelope.getType()).isEqualTo("id.payu.wallet.credited");
        }

        @Test
        @DisplayName("should accept PayU extensions")
        void payuExtensions() {
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/services/transaction-service"))
                    .type("id.payu.transaction.completed")
                    .payuTraceContext("00-trace123-span456-01")
                    .payuCorrelationId("corr-789")
                    .build();

            assertThat(envelope.getPayuTraceContext()).isEqualTo("00-trace123-span456-01");
            assertThat(envelope.getPayuCorrelationId()).isEqualTo("corr-789");
        }

        @Test
        @DisplayName("should accept custom subject")
        void customSubject() {
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/test"))
                    .type("test.event")
                    .subject("account-123")
                    .build();

            assertThat(envelope.getSubject()).isEqualTo("account-123");
        }

        @Test
        @DisplayName("should accept custom UUID")
        void customId() {
            UUID customId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/test"))
                    .type("test.event")
                    .id(customId)
                    .build();

            assertThat(envelope.getId()).isEqualTo(customId);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should pass validation with all required fields")
        void validEnvelope() {
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/test"))
                    .type("test.event")
                    .build();

            envelope.validate(); // should not throw
        }

        @Test
        @DisplayName("should fail validation when source is null")
        void nullSource() {
            CloudEventEnvelope<String> envelope = new CloudEventEnvelope<>();
            envelope.setType("test.event");

            assertThatThrownBy(envelope::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("source");
        }

        @Test
        @DisplayName("should fail validation when type is null")
        void nullType() {
            CloudEventEnvelope<String> envelope = new CloudEventEnvelope<>();
            envelope.setSource(URI.create("/test"));

            assertThatThrownBy(envelope::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("type");
        }

        @Test
        @DisplayName("should fail validation when type is blank")
        void blankType() {
            CloudEventEnvelope<String> envelope = new CloudEventEnvelope<>();
            envelope.setSource(URI.create("/test"));
            envelope.setType("  ");

            assertThatThrownBy(envelope::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("type");
        }
    }

    @Nested
    @DisplayName("JSON serialization")
    class JsonSerialization {

        private final ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @Test
        @DisplayName("should serialize to JSON with correct field names")
        void serializeToJson() throws Exception {
            UUID fixedId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .id(fixedId)
                    .source(URI.create("/services/test"))
                    .type("id.payu.test.created")
                    .data("test-payload")
                    .payuCorrelationId("corr-001")
                    .build();

            String json = mapper.writeValueAsString(envelope);

            assertThat(json).contains("\"specversion\":\"1.0\"");
            assertThat(json).contains("\"datacontenttype\":\"application/json\"");
            assertThat(json).contains("\"payucorrelationid\":\"corr-001\"");
            assertThat(json).contains("\"id\":\"550e8400-e29b-41d4-a716-446655440000\"");
        }

        @Test
        @DisplayName("should exclude null fields from JSON")
        void excludeNullFields() throws Exception {
            CloudEventEnvelope<String> envelope = CloudEventEnvelope.<String>builder()
                    .source(URI.create("/test"))
                    .type("test.event")
                    .build();

            String json = mapper.writeValueAsString(envelope);

            assertThat(json).doesNotContain("payutracecontext");
            assertThat(json).doesNotContain("payucorrelationid");
            assertThat(json).doesNotContain("subject");
        }

        @Test
        @DisplayName("should deserialize from JSON")
        void deserializeFromJson() throws Exception {
            String json = """
                    {
                        "specversion": "1.0",
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "source": "/services/test",
                        "type": "id.payu.test.created",
                        "datacontenttype": "application/json",
                        "data": "hello"
                    }
                    """;

            @SuppressWarnings("unchecked")
            CloudEventEnvelope<String> envelope = mapper.readValue(json, CloudEventEnvelope.class);

            assertThat(envelope.getSpecVersion()).isEqualTo("1.0");
            assertThat(envelope.getType()).isEqualTo("id.payu.test.created");
            assertThat(envelope.getSource()).isEqualTo(URI.create("/services/test"));
        }
    }
}
