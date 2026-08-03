package id.payu.billing.application.service;

import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionStatus;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.ChargeStatus;
import id.payu.billing.domain.model.SubscriptionPlan;
import id.payu.billing.domain.model.BillingInterval;
import id.payu.billing.domain.model.SubscriptionActor;
import id.payu.billing.domain.port.out.SubscriptionEventPort;
import id.payu.billing.domain.port.out.SubscriptionPersistencePort;
import id.payu.billing.exception.SubscriptionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Service Unit Tests")
class SubscriptionServiceTest {

    @InjectMocks
    SubscriptionService subscriptionService;

    @Mock
    SubscriptionPersistencePort persistencePort;

    @Mock
    SubscriptionEventPort eventPort;

    @Mock
    org.springframework.jms.core.JmsTemplate jmsTemplate;

    private SubscriptionPlan samplePlan;
    private UUID planId;
    private UUID subscriptionId;
    private SubscriptionActor actor;

    @BeforeEach
    void setup() {
        planId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        actor = new SubscriptionActor("user-1", "acc-001", "partner-nobar", true, false);

        samplePlan = new SubscriptionPlan();
        samplePlan.setId(planId);
        samplePlan.setPartnerId("partner-nobar");
        samplePlan.setPlanName("Premium Monthly");
        samplePlan.setDescription("Full access");
        samplePlan.setBillingInterval(BillingInterval.MONTHLY);
        samplePlan.setPrice(new BigDecimal("99000"));
        samplePlan.setCurrency("IDR");
        samplePlan.setTrialDays(7);
        samplePlan.setGracePeriodDays(3);
        samplePlan.setActive(true);
        samplePlan.setCreatedAt(LocalDateTime.now());
        samplePlan.setUpdatedAt(LocalDateTime.now());

        lenient().when(persistencePort.savePlan(any(SubscriptionPlan.class)))
                .thenAnswer(inv -> {
                    SubscriptionPlan p = inv.getArgument(0);
                    if (p.getId() == null) p.setId(UUID.randomUUID());
                    return p;
                });

        lenient().when(persistencePort.saveSubscription(any(Subscription.class)))
                .thenAnswer(inv -> {
                    Subscription s = inv.getArgument(0);
                    if (s.getId() == null) s.setId(UUID.randomUUID());
                    return s;
                });

        lenient().when(persistencePort.saveCharge(any(SubscriptionCharge.class)))
                .thenAnswer(inv -> {
                    SubscriptionCharge c = inv.getArgument(0);
                    if (c.getId() == null) c.setId(UUID.randomUUID());
                    return c;
                });

        // Don't fail tests on event publishing
        lenient().doNothing().when(eventPort).publishSubscriptionCreated(any(Subscription.class));
        lenient().doNothing().when(eventPort).publishChargeSucceeded(any(Subscription.class), any(SubscriptionCharge.class));
        lenient().doNothing().when(eventPort).publishChargeFailed(any(Subscription.class), any(SubscriptionCharge.class));
    }

    // ═══════════════════════════════════════════════════════
    //  Plan Management Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Plan Management")
    class PlanManagementTests {

        @Test
        @DisplayName("should reject creating a plan for another partner")
        void shouldRejectCrossPartnerPlanCreation() {
            assertThrows(AccessDeniedException.class, () -> subscriptionService.createPlan(
                    actor, "partner-other", "Premium", "Full access",
                    BillingInterval.MONTHLY, new BigDecimal("99000"), "IDR", 0, 0));

            verifyNoInteractions(persistencePort);
        }

        @Test
        @DisplayName("should create subscription plan successfully")
        void shouldCreatePlanSuccessfully() {
            SubscriptionPlan result = subscriptionService.createPlan(
                    actor, "partner-nobar", "Premium Monthly", "Full access",
                    BillingInterval.MONTHLY, new BigDecimal("99000"), "IDR", 7, 3);

            assertNotNull(result);
            assertEquals("partner-nobar", result.getPartnerId());
            assertEquals("Premium Monthly", result.getPlanName());
            assertEquals(BillingInterval.MONTHLY, result.getBillingInterval());
            assertEquals(new BigDecimal("99000"), result.getPrice());
            assertTrue(result.isActive());
            verify(persistencePort).savePlan(any(SubscriptionPlan.class));
        }

        @Test
        @DisplayName("should default currency to IDR when null")
        void shouldDefaultCurrencyToIDR() {
            subscriptionService.createPlan(
                    actor, "partner-nobar", "Basic", null,
                    BillingInterval.WEEKLY, new BigDecimal("25000"), null, 0, 0);

            ArgumentCaptor<SubscriptionPlan> captor = ArgumentCaptor.forClass(SubscriptionPlan.class);
            verify(persistencePort).savePlan(captor.capture());
            assertEquals("IDR", captor.getValue().getCurrency());
        }

        @Test
        @DisplayName("should get plan by ID")
        void shouldGetPlanById() {
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));

            SubscriptionPlan result = subscriptionService.getPlan(actor, planId);
            assertEquals(planId, result.getId());
            assertEquals("Premium Monthly", result.getPlanName());
        }

        @Test
        @DisplayName("should throw when plan not found")
        void shouldThrowWhenPlanNotFound() {
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.empty());

            SubscriptionNotFoundException error = assertThrows(SubscriptionNotFoundException.class,
                    () -> subscriptionService.getPlan(actor, planId));
            assertEquals("SubscriptionEntity plan not found: " + planId, error.getMessage());
        }

        @Test
        @DisplayName("should get plans by partner")
        void shouldGetPlansByPartner() {
            when(persistencePort.findPlansByPartnerId("partner-nobar"))
                    .thenReturn(List.of(samplePlan));

            List<SubscriptionPlan> result = subscriptionService.getPlansByPartner(actor, "partner-nobar");
            assertEquals(1, result.size());
            assertEquals("Premium Monthly", result.get(0).getPlanName());
        }

        @Test
        @DisplayName("should deactivate plan")
        void shouldDeactivatePlan() {
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));

            subscriptionService.deactivatePlan(actor, planId);

            assertFalse(samplePlan.isActive());
            verify(persistencePort).savePlan(samplePlan);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Subscription Lifecycle Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Subscription Lifecycle")
    class SubscriptionLifecycleTests {

        @Test
        @DisplayName("should reject subscribing another account")
        void shouldRejectCrossAccountSubscription() {
            assertThrows(AccessDeniedException.class,
                    () -> subscriptionService.subscribe(actor, "acc-other", planId, null));

            verifyNoInteractions(persistencePort);
        }

        @Test
        @DisplayName("should create subscription with trial period")
        void shouldCreateSubscriptionWithTrial() {
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));

            Subscription result = subscriptionService.subscribe(actor, "acc-001", planId, "ext-ref-1");

            assertNotNull(result);
            assertEquals("acc-001", result.getAccountId());
            assertEquals(planId, result.getPlanId());
            assertEquals(SubscriptionStatus.TRIAL, result.getStatus());
            assertNotNull(result.getTrialEndAt());
            assertNotNull(result.getNextBillingAt());
            assertEquals(new BigDecimal("99000"), result.getCurrentPrice());
        }

        @Test
        @DisplayName("should create subscription without trial (immediately active)")
        void shouldCreateSubscriptionWithoutTrial() {
            samplePlan.setTrialDays(0);
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));

            Subscription result = subscriptionService.subscribe(actor, "acc-001", planId, null);

            assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
            assertNotNull(result.getCurrentPeriodStart());
            assertNotNull(result.getCurrentPeriodEnd());
            assertNotNull(result.getNextBillingAt());
        }

        @Test
        @DisplayName("should reject subscription to inactive plan")
        void shouldRejectSubscriptionToInactivePlan() {
            samplePlan.setActive(false);
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> subscriptionService.subscribe(actor, "acc-001", planId, null));
            assertEquals("SubscriptionEntity plan is not active: " + planId, error.getMessage());
        }

        @Test
        @DisplayName("should get subscription by ID")
        void shouldGetSubscriptionById() {
            Subscription sub = createActiveSub();
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));

            Subscription result = subscriptionService.getSubscription(actor, subscriptionId);
            assertEquals(subscriptionId, result.getId());
        }

        @Test
        @DisplayName("should throw when subscription not found")
        void shouldThrowWhenSubNotFound() {
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.empty());

            SubscriptionNotFoundException error = assertThrows(SubscriptionNotFoundException.class,
                    () -> subscriptionService.getSubscription(actor, subscriptionId));
            assertEquals("SubscriptionEntity not found: " + subscriptionId, error.getMessage());
        }

        @Test
        @DisplayName("should get subscriptions by account")
        void shouldGetSubscriptionsByAccount() {
            Subscription sub = createActiveSub();
            when(persistencePort.findSubscriptionsByAccountId("acc-001"))
                    .thenReturn(List.of(sub));

            List<Subscription> result = subscriptionService.getSubscriptionsByAccount(actor, "acc-001");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("should cancel subscription")
        void shouldCancelSubscription() {
            Subscription sub = createActiveSub();
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));

            Subscription result = subscriptionService.cancelSubscription(actor, subscriptionId, "Too expensive");

            assertEquals(SubscriptionStatus.CANCELLED, result.getStatus());
            assertNotNull(result.getCancelledAt());
            assertEquals("Too expensive", result.getCancellationReason());
        }

        @Test
        @DisplayName("should reject cancelling already cancelled subscription")
        void shouldRejectDoubleCancellation() {
            Subscription sub = createActiveSub();
            sub.cancel("first");
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> subscriptionService.cancelSubscription(actor, subscriptionId, "second"));
            assertEquals("SubscriptionEntity is already cancelled", error.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Scheduled Billing & Dunning Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Scheduled Billing & Dunning")
    class ScheduledBillingTests {

        @Test
        @DisplayName("should process due subscriptions and charge them")
        void shouldProcessDueSubscriptions() {
            Subscription sub = createActiveSub();
            sub.setNextBillingAt(LocalDateTime.now().minusHours(1));
            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(List.of(sub));
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            int processed = subscriptionService.processDueSubscriptions();

            assertEquals(1, processed);
            verify(persistencePort).saveCharge(any(SubscriptionCharge.class));
            verify(persistencePort, atLeast(1)).saveSubscription(any(Subscription.class));
        }

        @Test
        @DisplayName("should skip duplicate charge via idempotency key")
        void shouldSkipDuplicateCharge() {
            Subscription sub = createActiveSub();
            sub.setNextBillingAt(LocalDateTime.now().minusHours(1));
            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(List.of(sub));
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findChargeByIdempotencyKey(any()))
                    .thenReturn(Optional.of(new SubscriptionCharge()));

            int processed = subscriptionService.processDueSubscriptions();

            assertEquals(1, processed);
            verify(persistencePort, never()).saveCharge(any(SubscriptionCharge.class));
        }

        @Test
        @DisplayName("should suspend subscription after dunning exhaustion (>=3 attempts)")
        void shouldSuspendAfterDunningExhaustion() {
            Subscription sub = createActiveSub();
            sub.setStatus(SubscriptionStatus.PAST_DUE);
            sub.setDunningAttempts(3);

            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(List.of(sub));

            subscriptionService.processDueSubscriptions();

            assertEquals(SubscriptionStatus.SUSPENDED, sub.getStatus());
            verify(persistencePort).saveSubscription(sub);
        }

        @Test
        @DisplayName("should return zero when no due subscriptions")
        void shouldReturnZeroWhenNoDue() {
            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(Collections.emptyList());

            int processed = subscriptionService.processDueSubscriptions();
            assertEquals(0, processed);
        }

        @Test
        @DisplayName("should process expired trials and activate them")
        void shouldProcessExpiredTrials() {
            Subscription sub = createTrialSub();
            when(persistencePort.findExpiredTrials(any(LocalDateTime.class)))
                    .thenReturn(List.of(sub));
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            int processed = subscriptionService.processExpiredTrials();

            assertEquals(1, processed);
            assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
            assertNotNull(sub.getCurrentPeriodStart());
            assertNotNull(sub.getCurrentPeriodEnd());
        }

        @Test
        @DisplayName("should return zero when no expired trials")
        void shouldReturnZeroWhenNoExpiredTrials() {
            when(persistencePort.findExpiredTrials(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            int processed = subscriptionService.processExpiredTrials();
            assertEquals(0, processed);
        }

        @Test
        @DisplayName("should process scheduled charge for ACTIVE subscription")
        void shouldProcessScheduledChargeForActiveSub() {
            Subscription sub = createActiveSub();
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            subscriptionService.processScheduledCharge(subscriptionId);

            verify(persistencePort).saveCharge(any(SubscriptionCharge.class));
            verify(jmsTemplate).convertAndSend(eq("payu.billing.scheduled"), eq(subscriptionId.toString()), any(org.springframework.jms.core.MessagePostProcessor.class));
        }

        @Test
        @DisplayName("should process scheduled charge for PAST_DUE subscription with retries left")
        void shouldProcessScheduledChargeForPastDueWithRetries() {
            Subscription sub = createActiveSub();
            sub.setStatus(SubscriptionStatus.PAST_DUE);
            sub.setDunningAttempts(1);
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            subscriptionService.processScheduledCharge(subscriptionId);

            verify(persistencePort).saveCharge(any(SubscriptionCharge.class));
        }

        @Test
        @DisplayName("should suspend PAST_DUE subscription when dunning exhausted")
        void shouldSuspendWhenDunningExhaustedOnScheduledCharge() {
            Subscription sub = createActiveSub();
            sub.setStatus(SubscriptionStatus.PAST_DUE);
            sub.setDunningAttempts(3);
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));

            subscriptionService.processScheduledCharge(subscriptionId);

            assertEquals(SubscriptionStatus.SUSPENDED, sub.getStatus());
            verify(persistencePort).saveSubscription(sub);
            verify(persistencePort, never()).saveCharge(any(SubscriptionCharge.class));
        }

        @Test
        @DisplayName("should skip scheduled charge for CANCELLED subscription")
        void shouldSkipScheduledChargeForCancelledSub() {
            Subscription sub = createActiveSub();
            sub.setStatus(SubscriptionStatus.CANCELLED);
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));

            subscriptionService.processScheduledCharge(subscriptionId);

            verify(persistencePort, never()).saveCharge(any(SubscriptionCharge.class));
            verify(jmsTemplate, never()).convertAndSend(anyString(), anyString(), any(org.springframework.jms.core.MessagePostProcessor.class));
        }

        @Test
        @DisplayName("should handle subscription not found for scheduled charge")
        void shouldHandleSubNotFoundForScheduledCharge() {
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.empty());

            // should not throw
            assertDoesNotThrow(() -> subscriptionService.processScheduledCharge(subscriptionId));
            verify(persistencePort, never()).saveCharge(any(SubscriptionCharge.class));
        }

        @Test
        @DisplayName("should schedule Artemis charge after successful billing")
        void shouldScheduleArtemisChargeAfterSuccess() {
            Subscription sub = createActiveSub();
            sub.setNextBillingAt(LocalDateTime.now().minusHours(1));
            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(List.of(sub));
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            subscriptionService.processDueSubscriptions();

            verify(jmsTemplate).convertAndSend(eq("payu.billing.scheduled"), eq(subscriptionId.toString()), any(org.springframework.jms.core.MessagePostProcessor.class));
        }

        @Test
        @DisplayName("should schedule dunning retry via Artemis when charge fails")
        void shouldScheduleDunningRetryViaArtemis() {
            Subscription sub = createActiveSub();
            sub.setNextBillingAt(LocalDateTime.now().minusHours(1));
            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(List.of(sub));
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            // Throw on first saveSubscription (triggers charge failure), then use default behavior
            doThrow(new RuntimeException("Wallet unavailable"))
                    .doAnswer(inv -> {
                        Subscription s = inv.getArgument(0);
                        if (s.getId() == null) s.setId(UUID.randomUUID());
                        return s;
                    })
                    .when(persistencePort).saveSubscription(any(Subscription.class));

            assertDoesNotThrow(() -> subscriptionService.processDueSubscriptions());

            // Dunning retry should be scheduled with 5 min delay
            verify(jmsTemplate).convertAndSend(eq("payu.billing.scheduled"), eq(subscriptionId.toString()), any(org.springframework.jms.core.MessagePostProcessor.class));
        }

        @Test
        @DisplayName("should continue processing even if Artemis scheduling fails")
        void shouldContinueProcessingWhenArtemisScheduleFails() {
            Subscription sub = createActiveSub();
            sub.setNextBillingAt(LocalDateTime.now().minusHours(1));
            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(List.of(sub));
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            doThrow(new RuntimeException("Artemis unavailable"))
                    .when(jmsTemplate).convertAndSend(anyString(), anyString(), any(org.springframework.jms.core.MessagePostProcessor.class));

            // Should not throw — Artemis is fire-and-forget
            assertDoesNotThrow(() -> subscriptionService.processDueSubscriptions());

            verify(persistencePort).saveCharge(any(SubscriptionCharge.class));
            verify(persistencePort, atLeast(1)).saveSubscription(any(Subscription.class));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Webhook Event Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Webhook Events")
    class WebhookEventTests {

        @Test
        @DisplayName("should publish subscription.created event on subscribe")
        void shouldPublishSubscriptionCreatedEvent() {
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));

            subscriptionService.subscribe(actor, "acc-001", planId, "ext-ref-1");

            verify(eventPort).publishSubscriptionCreated(any(Subscription.class));
        }

        @Test
        @DisplayName("should publish charge.succeeded event on successful charge")
        void shouldPublishChargeSucceededEvent() {
            Subscription sub = createActiveSub();
            sub.setNextBillingAt(LocalDateTime.now().minusHours(1));
            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(List.of(sub));
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            subscriptionService.processDueSubscriptions();

            verify(eventPort).publishChargeSucceeded(any(Subscription.class), any(SubscriptionCharge.class));
        }

        @Test
        @DisplayName("should publish charge.failed event on failed charge")
        void shouldPublishChargeFailedEvent() {
            Subscription sub = createActiveSub();
            sub.setNextBillingAt(LocalDateTime.now().minusHours(1));
            when(persistencePort.findDueSubscriptions(any(LocalDateTime.class)))
                    .thenReturn(List.of(sub));
            when(persistencePort.findPastDueSubscriptions())
                    .thenReturn(Collections.emptyList());
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            when(persistencePort.findChargeByIdempotencyKey(any())).thenReturn(Optional.empty());

            // Simulate charge failure by making saveSubscription throw on success case
            // but we need to verify the failure event is published
            java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
            doAnswer(inv -> {
                if (callCount.incrementAndGet() == 1) {
                    throw new RuntimeException("Wallet service unavailable");
                }
                Subscription s = inv.getArgument(0);
                if (s.getId() == null) s.setId(UUID.randomUUID());
                return s;
            }).when(persistencePort).saveSubscription(any(Subscription.class));

            // Should not throw, but log error
            assertDoesNotThrow(() -> subscriptionService.processDueSubscriptions());

            // The charge.failed event should be published before the exception
            verify(eventPort).publishChargeFailed(any(Subscription.class), any(SubscriptionCharge.class));
        }

        @Test
        @DisplayName("should continue subscription creation even if event publishing fails")
        void shouldContinueOnEventPublishFailure() {
            when(persistencePort.findPlanById(planId)).thenReturn(Optional.of(samplePlan));
            doThrow(new RuntimeException("Kafka unavailable"))
                    .when(eventPort).publishSubscriptionCreated(any(Subscription.class));

            Subscription result = subscriptionService.subscribe(actor, "acc-001", planId, "ext-ref-1");

            assertNotNull(result);
            assertEquals("acc-001", result.getAccountId());
            verify(persistencePort).saveSubscription(any(Subscription.class));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Charge History Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Charge History")
    class ChargeHistoryTests {

        @Test
        @DisplayName("should get charges by subscription")
        void shouldGetChargesBySubscription() {
            Subscription sub = createActiveSub();
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));
            SubscriptionCharge charge = new SubscriptionCharge();
            charge.setId(UUID.randomUUID());
            charge.setSubscriptionId(subscriptionId);
            charge.setAmount(new BigDecimal("99000"));
            charge.setStatus(ChargeStatus.SUCCEEDED);

            when(persistencePort.findChargesBySubscriptionId(subscriptionId))
                    .thenReturn(List.of(charge));

            List<SubscriptionCharge> result = subscriptionService
                    .getChargesBySubscription(actor, subscriptionId);

            assertEquals(1, result.size());
            assertEquals(ChargeStatus.SUCCEEDED, result.get(0).getStatus());
        }

        @Test
        @DisplayName("should return empty list when no charges")
        void shouldReturnEmptyWhenNoCharges() {
            Subscription sub = createActiveSub();
            when(persistencePort.findSubscriptionById(subscriptionId)).thenReturn(Optional.of(sub));
            when(persistencePort.findChargesBySubscriptionId(subscriptionId))
                    .thenReturn(Collections.emptyList());

            List<SubscriptionCharge> result = subscriptionService
                    .getChargesBySubscription(actor, subscriptionId);

            assertTrue(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════

    private Subscription createActiveSub() {
        Subscription sub = new Subscription();
        sub.setId(subscriptionId);
        sub.setAccountId("acc-001");
        sub.setPlanId(planId);
        sub.setPartnerId("partner-nobar");
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCurrentPrice(new BigDecimal("99000"));
        sub.setCurrency("IDR");
        sub.setCurrentPeriodStart(LocalDateTime.now().minusDays(30));
        sub.setCurrentPeriodEnd(LocalDateTime.now());
        sub.setNextBillingAt(LocalDateTime.now());
        sub.setDunningAttempts(0);
        sub.setCreatedAt(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        return sub;
    }

    private Subscription createTrialSub() {
        Subscription sub = new Subscription();
        sub.setId(subscriptionId);
        sub.setAccountId("acc-trial");
        sub.setPlanId(planId);
        sub.setPartnerId("partner-nobar");
        sub.setStatus(SubscriptionStatus.TRIAL);
        sub.setCurrentPrice(new BigDecimal("99000"));
        sub.setCurrency("IDR");
        sub.setTrialEndAt(LocalDateTime.now().minusHours(1));
        sub.setNextBillingAt(LocalDateTime.now().minusHours(1));
        sub.setDunningAttempts(0);
        sub.setCreatedAt(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        return sub;
    }
}
