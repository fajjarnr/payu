package id.payu.billing.application.service;

import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.domain.model.SubscriptionStatus;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;
import id.payu.billing.domain.model.ChargeStatus;
import id.payu.billing.adapter.persistence.entity.SubscriptionPlanEntity;
import id.payu.billing.domain.model.BillingInterval;
import id.payu.billing.domain.port.in.SubscriptionUseCase;
import id.payu.billing.domain.port.out.SubscriptionEventPort;
import id.payu.billing.domain.port.out.SubscriptionPersistencePort;
import id.payu.billing.exception.SubscriptionNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jms.core.JmsTemplate;

/**
 * Application service for subscription and recurring billing.
 * <p>
 * Key flows:
 * <ul>
 *   <li>Plan CRUD — partners define pricing + intervals</li>
 *   <li>Subscribe — user subscribes with optional trial</li>
 *   <li>Recurring charge — scheduler debits at nextBillingAt</li>
 *   <li>Dunning — retry failed charges up to 3 times, then suspend</li>
 *   <li>Cancel — explicit user cancellation</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionUseCase {

    private final SubscriptionPersistencePort persistencePort;
    private final SubscriptionEventPort eventPort;
    private final JmsTemplate jmsTemplate;

    // ═══════════════════════════════════════════════════════
    //  Plan Management
    // ═══════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "createPlanFallback")
    @Retry(name = "billing")
    @Transactional
    public SubscriptionPlanEntity createPlan(String partnerId, String planName, String description,
                                        BillingInterval interval, BigDecimal price, String currency,
                                        int trialDays, int gracePeriodDays) {
        log.info("Creating subscription plan: partner={}, name={}, interval={}, price={}",
                partnerId, planName, interval, price);

        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.setPartnerId(partnerId);
        plan.setPlanName(planName);
        plan.setDescription(description);
        plan.setBillingInterval(interval);
        plan.setPrice(price);
        plan.setCurrency(currency != null ? currency : "IDR");
        plan.setTrialDays(trialDays);
        plan.setGracePeriodDays(gracePeriodDays);
        plan.setActive(true);

        SubscriptionPlanEntity saved = persistencePort.savePlan(plan);
        log.info("SubscriptionEntity plan created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanEntity getPlan(UUID planId) {
        return persistencePort.findPlanById(planId)
                .orElseThrow(() -> new SubscriptionNotFoundException("SubscriptionEntity plan not found: " + planId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanEntity> getPlansByPartner(String partnerId) {
        return persistencePort.findPlansByPartnerId(partnerId);
    }

    @Override
    @Transactional
    public void deactivatePlan(UUID planId) {
        SubscriptionPlanEntity plan = getPlan(planId);
        plan.deactivate();
        persistencePort.savePlan(plan);
        log.info("SubscriptionEntity plan deactivated: id={}", planId);
    }

    // ═══════════════════════════════════════════════════════
    //  SubscriptionEntity Lifecycle
    // ═══════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "subscribeFallback")
    @Retry(name = "billing")
    @Transactional
    public SubscriptionEntity subscribe(String accountId, UUID planId, String externalReferenceId) {
        SubscriptionPlanEntity plan = getPlan(planId);
        if (!plan.isActive()) {
            throw new IllegalStateException("SubscriptionEntity plan is not active: " + planId);
        }

        log.info("Creating subscription: account={}, plan={}", maskId(accountId), plan.getPlanName());

        SubscriptionEntity sub = new SubscriptionEntity();
        sub.setAccountId(accountId);
        sub.setPlanId(planId);
        sub.setPartnerId(plan.getPartnerId());
        sub.setCurrentPrice(plan.getPrice());
        sub.setCurrency(plan.getCurrency());
        sub.setExternalReferenceId(externalReferenceId);

        LocalDateTime now = LocalDateTime.now();

        if (plan.getTrialDays() > 0) {
            // Start with trial
            sub.setStatus(SubscriptionStatus.TRIAL);
            sub.setTrialEndAt(now.plusDays(plan.getTrialDays()));
            sub.setNextBillingAt(now.plusDays(plan.getTrialDays()));
        } else {
            // No trial — immediately active, schedule first charge
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCurrentPeriodStart(now);
            sub.setCurrentPeriodEnd(advanceByInterval(now, plan.getBillingInterval()));
            sub.setNextBillingAt(advanceByInterval(now, plan.getBillingInterval()));
        }

        SubscriptionEntity saved = persistencePort.saveSubscription(sub);
        log.info("SubscriptionEntity created: id={}, status={}", saved.getId(), saved.getStatus());

        // Schedule next charge via Artemis delayed delivery
        scheduleArtemisCharge(saved);

        // Publish webhook event asynchronously
        try {
            eventPort.publishSubscriptionCreated(saved);
        } catch (Exception e) {
            log.warn("Failed to publish subscription.created event, subscription still created: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionEntity getSubscription(UUID subscriptionId) {
        return persistencePort.findSubscriptionById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("SubscriptionEntity not found: " + subscriptionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionEntity> getSubscriptionsByAccount(String accountId) {
        return persistencePort.findSubscriptionsByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionEntity> getSubscriptionsByPartner(String partnerId) {
        return persistencePort.findSubscriptionsByPartnerId(partnerId);
    }

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "cancelSubscriptionFallback")
    @Retry(name = "billing")
    @Transactional
    public SubscriptionEntity cancelSubscription(UUID subscriptionId, String reason) {
        SubscriptionEntity sub = getSubscription(subscriptionId);
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("SubscriptionEntity is already cancelled");
        }
        sub.cancel(reason);
        SubscriptionEntity saved = persistencePort.saveSubscription(sub);
        log.info("SubscriptionEntity cancelled: id={}, reason={}", subscriptionId, reason);
        return saved;
    }

    // ═══════════════════════════════════════════════════════
    //  Scheduled Billing & Dunning
    // ═══════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "processDueSubscriptionsFallback")
    @Retry(name = "billing")
    @SchedulerLock(name = "SubscriptionService_processDueSubscriptions", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")@Scheduled(fixedDelayString = "${payu.billing.subscription.charge-interval-ms:300000}")
    @Transactional
    public int processDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<SubscriptionEntity> dueSubscriptions = persistencePort.findDueSubscriptions(now);
        List<SubscriptionEntity> pastDue = persistencePort.findPastDueSubscriptions();

        int processed = 0;
        for (SubscriptionEntity sub : dueSubscriptions) {
            processCharge(sub);
            processed++;
        }

        // Dunning: retry past-due
        for (SubscriptionEntity sub : pastDue) {
            if (sub.isDunningExhausted()) {
                sub.suspend();
                persistencePort.saveSubscription(sub);
                log.warn("SubscriptionEntity suspended after dunning exhaustion: id={}", sub.getId());
            } else {
                processCharge(sub);
            }
            processed++;
        }

        if (processed > 0) {
            log.info("Processed {} due/past-due subscriptions", processed);
        }
        return processed;
    }

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "processExpiredTrialsFallback")
    @Retry(name = "billing")
    @SchedulerLock(name = "SubscriptionService_processExpiredTrials", lockAtLeastFor = "PT1S", lockAtMostFor = "PT10M")@Scheduled(fixedDelayString = "${payu.billing.subscription.trial-check-interval-ms:600000}")
    @Transactional
    public int processExpiredTrials() {
        LocalDateTime now = LocalDateTime.now();
        List<SubscriptionEntity> expired = persistencePort.findExpiredTrials(now);

        int processed = 0;
        for (SubscriptionEntity sub : expired) {
            log.info("Trial expired, activating subscription: id={}", sub.getId());
            // Transition from TRIAL to ACTIVE and schedule first charge
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCurrentPeriodStart(now);

            // Look up plan for interval
            SubscriptionPlanEntity plan = getPlan(sub.getPlanId());
            LocalDateTime periodEnd = advanceByInterval(now, plan.getBillingInterval());
            sub.setCurrentPeriodEnd(periodEnd);
            sub.setNextBillingAt(now); // charge immediately

            persistencePort.saveSubscription(sub);
            processCharge(sub);
            processed++;
        }

        if (processed > 0) {
            log.info("Processed {} expired trials", processed);
        }
        return processed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionChargeEntity> getChargesBySubscription(UUID subscriptionId) {
        return persistencePort.findChargesBySubscriptionId(subscriptionId);
    }

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private SubscriptionPlanEntity createPlanFallback(String partnerId, String planName, String description,
                                                BillingInterval interval, BigDecimal price, String currency,
                                                int trialDays, int gracePeriodDays, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for createPlan: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
    }

    private SubscriptionEntity subscribeFallback(String accountId, UUID planId, String externalReferenceId, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for subscribe: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
    }

    private SubscriptionEntity cancelSubscriptionFallback(UUID subscriptionId, String reason, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for cancelSubscription: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
    }

    private int processDueSubscriptionsFallback(Exception ex) {
        log.error("Fallback for processDueSubscriptions: {}", ex.getMessage());
        return 0;
    }

    private int processExpiredTrialsFallback(Exception ex) {
        log.error("Fallback for processExpiredTrials: {}", ex.getMessage());
        return 0;
    }

    // ═══════════════════════════════════════════════════════
    //  Internal Helpers
    // ═══════════════════════════════════════════════════════

    private void processCharge(SubscriptionEntity sub) {
        // BUG-BE-187 FIX: Skip charging subscriptions that are still in trial period
        if (sub.getStatus() == SubscriptionStatus.TRIAL) {
            log.info("Skipping charge for subscription {} — still in trial period (ends at {})",
                    sub.getId(), sub.getTrialEndAt());
            return;
        }

        String idempotencyKey = "sub-" + sub.getId() + "-" + sub.getNextBillingAt();

        // Idempotency check
        if (persistencePort.findChargeByIdempotencyKey(idempotencyKey).isPresent()) {
            log.debug("Charge already processed for idempotency key: {}", idempotencyKey);
            return;
        }

        SubscriptionChargeEntity charge = new SubscriptionChargeEntity();
        charge.setSubscriptionId(sub.getId());
        charge.setAccountId(sub.getAccountId());
        charge.setAmount(sub.getCurrentPrice());
        charge.setCurrency(sub.getCurrency());
        charge.setStatus(ChargeStatus.PENDING);
        charge.setAttemptNumber(sub.getDunningAttempts() + 1);
        charge.setIdempotencyKey(idempotencyKey);
        charge.setBillingPeriodStart(sub.getCurrentPeriodStart());
        charge.setBillingPeriodEnd(sub.getCurrentPeriodEnd());

        try {
            // In a full implementation, this would call WalletPort to debit the account.
            // For now, we record the charge as succeeded (integration with wallet-service
            // will be wired in a future story).
            charge.markSucceeded();
            persistencePort.saveCharge(charge);

            // Advance billing cycle
            SubscriptionPlanEntity plan = getPlan(sub.getPlanId());
            LocalDateTime nextStart = sub.getCurrentPeriodEnd() != null
                    ? sub.getCurrentPeriodEnd()
                    : LocalDateTime.now();
            LocalDateTime nextEnd = advanceByInterval(nextStart, plan.getBillingInterval());
            sub.activate(nextStart, nextEnd, nextEnd);
            persistencePort.saveSubscription(sub);

            log.info("Charge succeeded: subscription={}, amount={} {}", sub.getId(),
                    charge.getAmount(), charge.getCurrency());

            // Schedule next recurring charge via Artemis
            scheduleArtemisCharge(sub);

            // Publish webhook event for successful charge
            try {
                eventPort.publishChargeSucceeded(sub, charge);
            } catch (Exception ex) {
                log.warn("Failed to publish charge.succeeded event: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Charge failed: subscription={}, attempt={}", sub.getId(),
                    charge.getAttemptNumber(), e);
            charge.markFailed(e.getMessage());
            persistencePort.saveCharge(charge);

            sub.markPastDue();
            if (sub.isDunningExhausted()) {
                sub.suspend();
                persistencePort.saveSubscription(sub);
                log.warn("Subscription suspended after dunning exhaustion: {}", sub.getId());
            } else {
                persistencePort.saveSubscription(sub);
                // Schedule next dunning retry via Artemis (delay 5 mins for dunning retry)
                log.info("Scheduling dunning retry for subscription {} (dunning attempt {})", sub.getId(), sub.getDunningAttempts());
                try {
                    jmsTemplate.convertAndSend("payu.billing.scheduled", sub.getId().toString(), m -> {
                        m.setLongProperty("_AMQ_SCHED_DELIVERY", System.currentTimeMillis() + 300000L);
                        return m;
                    });
                } catch (Exception ex) {
                    log.error("Failed to schedule Artemis dunning retry: {}", ex.getMessage());
                }
            }

            // Publish webhook event for failed charge (dunning)
            try {
                eventPort.publishChargeFailed(sub, charge);
            } catch (Exception ex) {
                log.warn("Failed to publish charge.failed event: {}", ex.getMessage());
            }
        }
    }

    /**
     * Helper to schedule subscription billing command to Artemis with delay.
     */
    private void scheduleArtemisCharge(SubscriptionEntity sub) {
        if (sub.getStatus() == SubscriptionStatus.CANCELLED || sub.getStatus() == SubscriptionStatus.SUSPENDED) {
            return;
        }
        LocalDateTime nextBilling = sub.getNextBillingAt();
        if (nextBilling != null) {
            long delayMs = java.time.Duration.between(LocalDateTime.now(), nextBilling).toMillis();
            final long finalDelayMs = delayMs < 0 ? 0 : delayMs;
            log.info("Scheduling charge for subscription {} at {} (delay: {}ms)", sub.getId(), nextBilling, finalDelayMs);
            try {
                jmsTemplate.convertAndSend("payu.billing.scheduled", sub.getId().toString(), m -> {
                    m.setLongProperty("_AMQ_SCHED_DELIVERY", System.currentTimeMillis() + finalDelayMs);
                    return m;
                });
            } catch (Exception e) {
                log.error("Failed to schedule Artemis charge for subscription: {}", sub.getId(), e);
            }
        }
    }

    /**
     * Entry point to charge a subscription from an Artemis scheduled event.
     */
    @Transactional
    public void processScheduledCharge(UUID subscriptionId) {
        log.info("Processing scheduled charge from Artemis for subscription: {}", subscriptionId);
        persistencePort.findSubscriptionById(subscriptionId).ifPresentOrElse(
            sub -> {
                if (sub.getStatus() == SubscriptionStatus.ACTIVE || sub.getStatus() == SubscriptionStatus.TRIAL) {
                    processCharge(sub);
                } else if (sub.getStatus() == SubscriptionStatus.PAST_DUE) {
                    if (sub.isDunningExhausted()) {
                        sub.suspend();
                        persistencePort.saveSubscription(sub);
                        log.warn("Subscription suspended after dunning exhaustion: {}", sub.getId());
                    } else {
                        processCharge(sub);
                    }
                } else {
                    log.info("Subscription {} is in status {}, skipping charge", sub.getId(), sub.getStatus());
                }
            },
            () -> log.warn("Subscription not found for scheduled charge: {}", subscriptionId)
        );
    }

    private LocalDateTime advanceByInterval(LocalDateTime from, BillingInterval interval) {
        return switch (interval) {
            case DAILY -> from.plusDays(1);
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
            case YEARLY -> from.plusYears(1);
        };
    }

    private String maskId(String id) {
        if (id == null || id.length() < 8) return "***";
        return id.substring(0, 4) + "****" + id.substring(id.length() - 4);
    }
}
