package id.payu.transaction.config;

import id.payu.saga.annotation.EnableSaga;
import org.springframework.context.annotation.Configuration;

/**
 * Enables PayU Saga Starter for distributed transaction orchestration.
 * <p>
 * The saga orchestrator manages the BiFast/SKN/RGS transfer lifecycle:
 * 1. Reserve wallet balance
 * 2. Initiate external transfer
 * 3. Commit balance on success / compensate on failure
 * 4. Publish domain events
 * <p>
 * Saga instances are persisted to PostgreSQL for recovery and audit trail.
 *
 * @see id.payu.transaction.application.saga.TransferSagaOrchestrator
 */
@Configuration
@EnableSaga(
        basePackages = "id.payu.transaction",
        enablePersistence = true,
        enableMonitoring = true,
        enableCompensation = true
)
public class SagaConfig {
}
