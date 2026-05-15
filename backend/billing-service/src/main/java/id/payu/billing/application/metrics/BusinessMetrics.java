package id.payu.billing.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    private final Counter paymentsInitiated;
    private final Counter paymentsCompleted;
    private final Counter paymentsFailed;

    public BusinessMetrics(MeterRegistry registry) {
        this.paymentsInitiated = Counter.builder("payu.billing.payments.initiated")
                .description("Number of billing payments initiated").register(registry);
        this.paymentsCompleted = Counter.builder("payu.billing.payments.completed")
                .description("Number of billing payments completed successfully").register(registry);
        this.paymentsFailed = Counter.builder("payu.billing.payments.failed")
                .description("Number of billing payments that failed").register(registry);
    }

    public void recordPaymentInitiated() { paymentsInitiated.increment(); }
    public void recordPaymentCompleted() { paymentsCompleted.increment(); }
    public void recordPaymentFailed() { paymentsFailed.increment(); }
}
