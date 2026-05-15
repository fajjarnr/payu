package id.payu.partner.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    private final MeterRegistry registry;
    private final Counter webhooksDelivered;
    private final Counter webhooksFailed;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.webhooksDelivered = Counter.builder("payu.partner.webhooks.delivered")
                .description("Number of webhooks successfully delivered").register(registry);
        this.webhooksFailed = Counter.builder("payu.partner.webhooks.failed")
                .description("Number of webhook deliveries that failed").register(registry);
    }

    public void recordApiCall(String partner, String endpoint) {
        Counter.builder("payu.partner.api.calls").description("Number of partner API calls")
                .tag("partner", partner).tag("endpoint", endpoint).register(registry).increment();
    }
    public void recordWebhookDelivered() { webhooksDelivered.increment(); }
    public void recordWebhookFailed() { webhooksFailed.increment(); }
}
