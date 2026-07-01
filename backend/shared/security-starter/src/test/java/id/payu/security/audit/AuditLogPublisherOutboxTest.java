package id.payu.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.outbox.service.OutboxService;
import id.payu.security.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * AUDIT-049 fix: {@link AuditLogPublisher} must always publish audit events via
 * {@link OutboxService} (Rule #4). The previous implementation kept a
 * {@code kafkaTemplate.send()} fallback path that silently bypassed outbox
 * whenever the {@code OutboxService} bean was missing — violating
 * <em>AGENTS.md</em> rule #4 for compliance-critical audit logs.
 */
class AuditLogPublisherOutboxTest {

    private static final String TOPIC = "payu.security.audit-log.v1";

    private SecurityProperties propertiesWithAuditEnabled() {
        SecurityProperties props = new SecurityProperties();
        // Audit config defaults to enabled=true with operations including "LOGIN".
        return props;
    }

    @Test
    void shouldUseOutboxWhenProvidedAndSkipKafkaTemplateEntirely() {
        OutboxService outboxService = mock(OutboxService.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SecurityProperties properties = propertiesWithAuditEnabled();

        AuditLogPublisher publisher = new AuditLogPublisher(
            properties, kafkaTemplate, objectMapper, outboxService);
        publisher.publish(AuditEvent.builder().eventType("LOGIN").userId("u-1").build());

        verify(outboxService).createEvent(
            eq("AuditLog"),
            any(),
            eq("LOGIN"),
            any(),
            isNull(),
            eq(TOPIC)
        );
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldFailFastWhenOutboxServiceMissingInsteadOfFallingBackToKafka() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SecurityProperties properties = propertiesWithAuditEnabled();

        // 3-arg constructor (legacy): no OutboxService wired.
        AuditLogPublisher publisher = new AuditLogPublisher(
            properties, kafkaTemplate, objectMapper);

        AuditEvent event = AuditEvent.builder()
            .eventType("LOGIN")
            .userId("u-1")
            .build();

        // Currently this would call kafkaTemplate.send() silently.
        // After AUDIT-049 fix, publish() must fail-fast with IllegalStateException
        // and never touch KafkaTemplate.
        assertThatThrownBy(() -> publisher.publish(event))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OutboxService");

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldNotAttemptOutboxWhenAuditDisabled() {
        OutboxService outboxService = mock(OutboxService.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SecurityProperties properties = new SecurityProperties();
        properties.getAudit().setEnabled(false);

        AuditLogPublisher publisher = new AuditLogPublisher(
            properties, null, objectMapper, outboxService);
        publisher.publish(AuditEvent.builder().eventType("LOGIN").build());

        verifyNoInteractions(outboxService);
    }
}
