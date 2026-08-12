package id.payu.billing.application.service;

import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionStatus;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.ChargeStatus;
import id.payu.billing.domain.model.SubscriptionPlan;
import id.payu.billing.domain.model.BillingInterval;
import id.payu.billing.domain.model.SubscriptionActor;
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
    private final id.payu.billing.domain.port.out.WalletPort walletPort;

    // ═══════════════════════════════════════════════════════
    //  Plan Management
    // ═══════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "createPlanFallback")
    @Retry(name = "billing")
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionActor actor, String partnerId, String planName, String description,
                                        BillingInterval interval, BigDecimal price, String currency,
                                        int trialDays, int gracePeriodDays) {
        requirePartnerOwner(actor, partnerId);
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
    public SubscriptionPlan getPlan(SubscriptionActor actor, UUID planId) {
        requireAuthenticated(actor);
        return findPlan(planId);
    }

    private SubscriptionPlan findPlan(UUID planId) {
        return persistencePort.findPlanById(planId)
                .orElseThrow(() -> new SubscriptionNotFoundException("SubscriptionEntity plan not found: " + planId));
    }

    private Subscription findSubscription(UUID subscriptionId) {
        return persistencePort.findSubscriptionById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("SubscriptionEntity not found: " + subscriptionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlan> getPlansByPartner(SubscriptionActor actor, String partnerId) {
        requirePartnerOwner(actor, partnerId);
        return persistencePort.findPlansByPartnerId(partnerId);
    }

    @Override
    @Transactional
    public void deactivatePlan(SubscriptionActor actor, UUID planId) {
        SubscriptionPlan plan = findPlan(planId);
        requirePartnerOwner(actor, plan.getPartnerId());
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
    public Subscription subscribe(SubscriptionActor actor, String accountId, UUID planId, String externalReferenceId) {
        requireAccountOwner(actor, accountId);
        SubscriptionPlan plan = findPlan(planId);
        if (!plan.isActive()) {
            throw new IllegalStateException("SubscriptionEntity plan is not active: " + planId);
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
    public Subscription getSubscription(SubscriptionActor actor, UUID subscriptionId) {
        Subscription sub = findSubscription(subscriptionId);
        requireAccountOwner(actor, sub.getAccountId());
        return sub;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByAccount(SubscriptionActor actor, String accountId) {
        requireAccountOwner(actor, accountId);
        return persistencePort.findSubscriptionsByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByPartner(SubscriptionActor actor, String partnerId) {
        requirePartnerOwner(actor, partnerId);
        return persistencePort.findSubscriptionsByPartnerId(partnerId);
    }

    @Override
    @CircuitBreaker(name = "billing", fallbackMethod = "cancelSubscriptionFallback")
    @Retry(name = "billing")
    @Transactional
    public Subscription cancelSubscription(SubscriptionActor actor, UUID subscriptionId, String reason) {
        Subscription sub = findSubscription(subscriptionId);
        requireAccountOwner(actor, sub.getAccountId());
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("SubscriptionEntity is already cancelled");
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
    @SchedulerLock(name = "SubscriptionService_processDueSubscriptions", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")@Scheduled(fixedDelayString = "${payu.billing.subscription.charge-interval-ms:300000}")
    @Transactional
    public Integer processDueSubscriptions() {
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
    @SchedulerLock(name = "SubscriptionService_processExpiredTrials", lockAtLeastFor = "PT1S", lockAtMostFor = "PT10M")@Scheduled(fixedDelayString = "${payu.billing.subscription.trial-check-interval-ms:600000}")
    @Transactional
    public Integer processExpiredTrials() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expired = persistencePort.findExpiredTrials(now);

        int processed = 0;
        for (Subscription sub : expired) {
            log.info("Trial expired, activating subscription: id={}", sub.getId());
            // Transition from TRIAL to ACTIVE and schedule first charge
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCurrentPeriodStart(now);

            // Look up plan for interval
            SubscriptionPlan plan = findPlan(sub.getPlanId());
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
    public List<SubscriptionCharge> getChargesBySubscription(SubscriptionActor actor, UUID subscriptionId) {
        Subscription sub = findSubscription(subscriptionId);
        requireAccountOwner(actor, sub.getAccountId());
        return persistencePort.findChargesBySubscriptionId(subscriptionId);
    }

    private void requireAuthenticated(SubscriptionActor actor) {
        if (actor == null || actor.subject() == null || actor.subject().isBlank()) {
            throw new AccessDeniedException("Authenticated subject is required");
        }
    }

    private void requirePartnerOwner(SubscriptionActor actor, String partnerId) {
        requireAuthenticated(actor);
        if (!actor.canManagePartner(partnerId)) {
            throw new AccessDeniedException("Partner access denied");
        }
    }

    private void requireAccountOwner(SubscriptionActor actor, String accountId) {
        requireAuthenticated(actor);
        if (!actor.canAccessAccount(accountId)) {
            throw new AccessDeniedException("Account access denied");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private SubscriptionPlan createPlanFallback(SubscriptionActor actor, String partnerId, String planName, String description,
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

    private Subscription subscribeFallback(SubscriptionActor actor, String accountId, UUID planId, String externalReferenceId, Exception ex) {
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

    private Subscription cancelSubscriptionFallback(SubscriptionActor actor, UUID subscriptionId, String reason, Exception ex) {
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

    private Integer processDueSubscriptionsFallback(Exception ex) {
        log.error("Fallback for processDueSubscriptions: {}", ex.getMessage());
        return 0;
    }

    private Integer processExpiredTrialsFallback(Exception ex) {
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
            // SUB-001: mark succeeded ONLY after the wallet debit is committed.
            // Reserve-then-commit keeps the charge idempotent via the charge's
            // own idempotency key as the wallet reference.
            id.payu.billing.domain.port.out.WalletPort.ReserveResult reservation =
                    walletPort.reserveBalance(sub.getAccountId(), charge.getAmount(), idempotencyKey);
            if (reservation == null || !"RESERVED".equals(reservation.status()) || reservation.reservationId() == null) {
                throw new IllegalStateException("Insufficient balance or wallet reserve failed");
            }
            try {
                walletPort.commitReservation(reservation.reservationId());
            } catch (Exception commitError) {
                try {
                    walletPort.releaseReservation(reservation.reservationId());
                } catch (Exception releaseError) {
                    log.error("CRITICAL: failed to release reservation {} after commit failure: {}",
                            reservation.reservationId(), releaseError.getMessage());
                }
                throw commitError;
            }
            charge.markSucceeded();
            persistencePort.saveCharge(charge);

            // Advance billing cycle
            SubscriptionPlan plan = findPlan(sub.getPlanId());
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
    private void scheduleArtemisCharge(Subscription sub) {
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
