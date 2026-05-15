package id.payu.billing.application.service;

import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.Subscription.SubscriptionStatus;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.SubscriptionCharge.ChargeStatus;
import id.payu.billing.domain.model.SubscriptionPlan;
import id.payu.billing.domain.model.SubscriptionPlan.BillingInterval;
import id.payu.billing.domain.port.in.SubscriptionUseCase;
import id.payu.billing.domain.port.out.SubscriptionEventPort;
import id.payu.billing.domain.port.out.SubscriptionPersistencePort;
import id.payu.billing.exception.SubscriptionNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    // ═══════════════════════════════════════════════════════
    //  Plan Management
    // ═══════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "createPlanFallback")
    @Retry(name = "billing")
    @Transactional
    public SubscriptionPlan createPlan(String partnerId, String planName, String description,
                                        BillingInterval interval, BigDecimal price, String currency,
                                        int trialDays, int gracePeriodDays) {
        log.info("Creating subscription plan: partner={}, name={}, interval={}, price={}",
                partnerId, planName, interval, price);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setPartnerId(partnerId);
        plan.setPlanName(planName);
        plan.setDescription(description);
        plan.setBillingInterval(interval);
        plan.setPrice(price);
        plan.setCurrency(currency != null ? currency : "IDR");
        plan.setTrialDays(trialDays);
        plan.setGracePeriodDays(gracePeriodDays);
        plan.setActive(true);

        SubscriptionPlan saved = persistencePort.savePlan(plan);
        log.info("Subscription plan created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlan getPlan(UUID planId) {
        return persistencePort.findPlanById(planId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription plan not found: " + planId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlan> getPlansByPartner(String partnerId) {
        return persistencePort.findPlansByPartnerId(partnerId);
    }

    @Override
    @Transactional
    public void deactivatePlan(UUID planId) {
        SubscriptionPlan plan = getPlan(planId);
        plan.deactivate();
        persistencePort.savePlan(plan);
        log.info("Subscription plan deactivated: id={}", planId);
    }

    // ═══════════════════════════════════════════════════════
    //  Subscription Lifecycle
    // ═══════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "subscribeFallback")
    @Retry(name = "billing")
    @Transactional
    public Subscription subscribe(String accountId, UUID planId, String externalReferenceId) {
        SubscriptionPlan plan = getPlan(planId);
        if (!plan.isActive()) {
            throw new IllegalStateException("Subscription plan is not active: " + planId);
        }

        log.info("Creating subscription: account={}, plan={}", maskId(accountId), plan.getPlanName());

        Subscription sub = new Subscription();
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

        Subscription saved = persistencePort.saveSubscription(sub);
        log.info("Subscription created: id={}, status={}", saved.getId(), saved.getStatus());

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
    public Subscription getSubscription(UUID subscriptionId) {
        return persistencePort.findSubscriptionById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + subscriptionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByAccount(String accountId) {
        return persistencePort.findSubscriptionsByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByPartner(String partnerId) {
        return persistencePort.findSubscriptionsByPartnerId(partnerId);
    }

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "cancelSubscriptionFallback")
    @Retry(name = "billing")
    @Transactional
    public Subscription cancelSubscription(UUID subscriptionId, String reason) {
        Subscription sub = getSubscription(subscriptionId);
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Subscription is already cancelled");
        }
        sub.cancel(reason);
        Subscription saved = persistencePort.saveSubscription(sub);
        log.info("Subscription cancelled: id={}, reason={}", subscriptionId, reason);
        return saved;
    }

    // ═══════════════════════════════════════════════════════
    //  Scheduled Billing & Dunning
    // ═══════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "processDueSubscriptionsFallback")
    @Retry(name = "billing")
    @Scheduled(fixedDelayString = "${payu.billing.subscription.charge-interval-ms:300000}")
    @Transactional
    public int processDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> dueSubscriptions = persistencePort.findDueSubscriptions(now);
        List<Subscription> pastDue = persistencePort.findPastDueSubscriptions();

        int processed = 0;
        for (Subscription sub : dueSubscriptions) {
            processCharge(sub);
            processed++;
        }

        // Dunning: retry past-due
        for (Subscription sub : pastDue) {
            if (sub.isDunningExhausted()) {
                sub.suspend();
                persistencePort.saveSubscription(sub);
                log.warn("Subscription suspended after dunning exhaustion: id={}", sub.getId());
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
    @Scheduled(fixedDelayString = "${payu.billing.subscription.trial-check-interval-ms:600000}")
    @Transactional
    public int processExpiredTrials() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expired = persistencePort.findExpiredTrials(now);

        int processed = 0;
        for (Subscription sub : expired) {
            log.info("Trial expired, activating subscription: id={}", sub.getId());
            // Transition from TRIAL to ACTIVE and schedule first charge
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCurrentPeriodStart(now);

            // Look up plan for interval
            SubscriptionPlan plan = getPlan(sub.getPlanId());
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
    public List<SubscriptionCharge> getChargesBySubscription(UUID subscriptionId) {
        return persistencePort.findChargesBySubscriptionId(subscriptionId);
    }

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private SubscriptionPlan createPlanFallback(String partnerId, String planName, String description,
                                                BillingInterval interval, BigDecimal price, String currency,
                                                int trialDays, int gracePeriodDays, Exception ex) {
        log.error("Fallback for createPlan: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
    }

    private Subscription subscribeFallback(String accountId, UUID planId, String externalReferenceId, Exception ex) {
        log.error("Fallback for subscribe: {}", ex.getMessage());
        throw new RuntimeException("Billing service temporarily unavailable", ex);
    }

    private Subscription cancelSubscriptionFallback(UUID subscriptionId, String reason, Exception ex) {
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

    private void processCharge(Subscription sub) {
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

        SubscriptionCharge charge = new SubscriptionCharge();
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
            SubscriptionPlan plan = getPlan(sub.getPlanId());
            LocalDateTime nextStart = sub.getCurrentPeriodEnd() != null
                    ? sub.getCurrentPeriodEnd()
                    : LocalDateTime.now();
            LocalDateTime nextEnd = advanceByInterval(nextStart, plan.getBillingInterval());
            sub.activate(nextStart, nextEnd, nextEnd);
            persistencePort.saveSubscription(sub);

            log.info("Charge succeeded: subscription={}, amount={} {}", sub.getId(),
                    charge.getAmount(), charge.getCurrency());

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
            persistencePort.saveSubscription(sub);

            // Publish webhook event for failed charge (dunning)
            try {
                eventPort.publishChargeFailed(sub, charge);
            } catch (Exception ex) {
                log.warn("Failed to publish charge.failed event: {}", ex.getMessage());
            }
        }
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
