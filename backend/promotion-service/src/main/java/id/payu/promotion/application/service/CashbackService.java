package id.payu.promotion.application.service;

import id.payu.promotion.domain.port.in.CashbackSagaUseCase;
import id.payu.promotion.domain.model.CashbackCommand;
import id.payu.promotion.domain.model.Cashback;
import id.payu.promotion.interfaces.dto.CreateCashbackRequest;
import id.payu.promotion.interfaces.dto.CashbackSummaryResponse;
import id.payu.promotion.domain.port.out.CashbackPersistencePort;
import id.payu.saga.model.SagaResult;
import id.payu.saga.model.SagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import id.payu.promotion.domain.port.out.CashbackEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.CashbackStatus;

/**
 * Service for managing cashback operations.
 * Uses Saga pattern to ensure atomicity between wallet credit and cashback record creation.
 */
@Service
public class CashbackService {

    private static final Logger LOG = LoggerFactory.getLogger(CashbackService.class);

    private final CashbackPersistencePort cashbackRepository;
    private final CashbackSagaUseCase sagaOrchestrator;
    private final CashbackEventPublisher eventPublisher;
    private final String promotionEventsTopic;
    private final MeterRegistry meterRegistry;

    public CashbackService(
            CashbackPersistencePort cashbackRepository,
            CashbackSagaUseCase sagaOrchestrator,
            CashbackEventPublisher eventPublisher,
            @Value("${app.kafka.topics.promotion-events:payu.promotion.cashback-event.v1}") String promotionEventsTopic,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.cashbackRepository = cashbackRepository;
        this.sagaOrchestrator = sagaOrchestrator;
        this.eventPublisher = eventPublisher;
        this.promotionEventsTopic = promotionEventsTopic;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Create cashback using Saga pattern to ensure atomicity.
     * Steps:
     * 1. Credit wallet via wallet-service
     * 2. Create cashback record with CREDITED status (only if step 1 succeeds)
     *
     * @param request the cashback creation request
     * @return the created cashback
     * @throws CashbackCreationException if saga execution fails
     */
    @Transactional
    public Cashback createCashback(CreateCashbackRequest request) {
        if (request.accountId() == null || request.accountId().isBlank()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        LOG.info("Creating cashback with saga: accountId={}, transactionId={}",
            request.accountId(), request.transactionId());

        // Create saga context
        var result = sagaOrchestrator.execute(new CashbackCommand(request.accountId(), request.transactionId(),
            request.transactionAmount(), request.merchantCode(), request.categoryCode(), request.cashbackCode()));

        if (result.success()) {
            Cashback cashback = result.cashback();
            LOG.info("CashbackEntity saga completed successfully: id={}, amount={}",
                cashback.getId(), cashback.getCashbackAmount());

            publishCashbackEvent(cashback);
            return cashback;
        } else {
            LOG.error("CashbackEntity saga failed: state={}, error={}, step={}",
                result.state(), result.error(), result.errorStep());

            // If saga was compensated, the cashback might be in PENDING or VOIDED state
            if (result.compensated() && result.cashback() != null) {
                return result.cashback();
            }

            throw new CashbackCreationException(
                "Failed to create cashback: " + result.error(),
                result.errorStep()
            );
        }
    }

    public Optional<Cashback> getCashback(UUID id) {
        return cashbackRepository.findById(id);
    }

    public List<Cashback> getCashbacksByAccount(String accountId) {
        return cashbackRepository.findByAccountId(accountId);
    }

    public CashbackSummaryResponse getCashbackSummary(String accountId) {
        List<Cashback> cashbacks = cashbackRepository.findByAccountId(accountId);

        BigDecimal totalCashback = cashbacks.stream()
            .map(Cashback::getCashbackAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingCashback = cashbacks.stream()
            .filter(c -> c.getStatus() == CashbackStatus.PENDING)
            .map(Cashback::getCashbackAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditedCashback = cashbacks.stream()
            .filter(c -> c.getStatus() == CashbackStatus.CREDITED)
            .map(Cashback::getCashbackAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int transactionCount = cashbacks.size();

        return new CashbackSummaryResponse(
            totalCashback != null ? totalCashback : BigDecimal.ZERO,
            pendingCashback != null ? pendingCashback : BigDecimal.ZERO,
            creditedCashback != null ? creditedCashback : BigDecimal.ZERO,
            transactionCount
        );
    }

    // MSG-008: Migrated to OutboxService for transactional atomicity
    private void publishCashbackEvent(Cashback cashback) {
        eventPublisher.publish(
                "Cashback",
                cashback.getId().toString(),
                "CashbackCreated",
                Map.of(
                        "cashbackId", cashback.getId().toString(),
                        "accountId", cashback.getAccountId(),
                        "amount", cashback.getCashbackAmount().toString(),
                        "status", cashback.getStatus().name(),
                        "timestamp", LocalDateTime.now().toString()
                ),
                promotionEventsTopic
        );
    }

    /**
     * Exception thrown when cashback creation fails.
     */
    public static class CashbackCreationException extends RuntimeException {
        private final String failedStep;

        public CashbackCreationException(String message, String failedStep) {
            super(message);
            this.failedStep = failedStep;
        }

        public String getFailedStep() {
            return failedStep;
        }
    }
}
