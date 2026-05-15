package id.payu.wallet.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    private final MeterRegistry registry;
    private final Counter reservationsCreated;
    private final Counter reservationsCommitted;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.reservationsCreated = Counter.builder("payu.wallet.reservations.created")
                .description("Number of wallet balance reservations created").register(registry);
        this.reservationsCommitted = Counter.builder("payu.wallet.reservations.committed")
                .description("Number of wallet balance reservations committed").register(registry);
    }

    public void recordCredit(String type) {
        Counter.builder("payu.wallet.credits").description("Number of wallet credit operations")
                .tag("type", type).register(registry).increment();
    }
    public void recordDebit(String type) {
        Counter.builder("payu.wallet.debits").description("Number of wallet debit operations")
                .tag("type", type).register(registry).increment();
    }
    public void recordReservationCreated() { reservationsCreated.increment(); }
    public void recordReservationCommitted() { reservationsCommitted.increment(); }
}
