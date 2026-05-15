package id.payu.transaction.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BusinessMetrics {
    private final MeterRegistry registry;
    private final Counter transactionsCompleted;
    private final Counter transactionsFailed;
    private final AtomicLong pendingCount = new AtomicLong(0);

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.transactionsCompleted = Counter.builder("payu.transactions.completed")
                .description("Number of transactions completed successfully").register(registry);
        this.transactionsFailed = Counter.builder("payu.transactions.failed")
                .description("Number of transactions that failed").register(registry);
        registry.gauge("payu.transactions.pending.count", pendingCount);
    }

    public void recordTransactionInitiated(String type, String status) {
        Counter.builder("payu.transactions.initiated").description("Number of transactions initiated")
                .tag("type", type).tag("status", status).register(registry).increment();
    }
    public void recordTransactionCompleted() { transactionsCompleted.increment(); }
    public void recordTransactionFailed() { transactionsFailed.increment(); }
    public void incrementPending() { pendingCount.incrementAndGet(); }
    public void decrementPending() { pendingCount.decrementAndGet(); }
}
