package id.payu.gateway.application.service;

import id.payu.gateway.application.service.GatewaySchedulerLockService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages checkout tokens for hosted checkout pages (Snap-style).
 * Partners create a token, embed/redirect to checkout page, customer pays.
 *
 * Part of E-15 IMP-043: Hosted Checkout Page
 */
@ApplicationScoped
public class CheckoutService {

    // In-memory store for checkout tokens (production would use Redis)
    private final ConcurrentHashMap<String, CheckoutSession> sessions = new ConcurrentHashMap<>();

    @Inject
    GatewaySchedulerLockService schedulerLock;

    /**
     * Cleanup expired sessions every 10 minutes to prevent unbounded memory growth.
     * ADR-0042: distributed lock
     */
    @Scheduled(every = "10m")
    void cleanupExpiredSessions() {
        if (!schedulerLock.tryAcquire("gateway-checkout-cleanupExpiredSessions", Duration.ofMinutes(5))) {
            return;
        }
        int before = sessions.size();
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(Instant.now()));
        int removed = before - sessions.size();
        if (removed > 0) {
            Log.infof("Cleaned up %d expired checkout sessions, %d remaining", removed, sessions.size());
        }
    }

    /**
     * Create a checkout token for hosted checkout.
     */
    public CheckoutSession createCheckoutToken(CreateCheckoutRequest request) {
        String token = "snap-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        CheckoutSession session = new CheckoutSession(
                token,
                request.partnerId(),
                request.orderId(),
                request.amount(),
                request.currency() != null ? request.currency() : "IDR",
                request.itemName(),
                request.customerName(),
                request.customerEmail(),
                request.callbackUrl(),
                request.redirectUrl(),
                "PENDING",
                null,
                null,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                generateCheckoutUrl(token)
        );

        sessions.put(token, session);
        Log.infof("Created checkout token %s for partner=%s order=%s amount=%s",
                token, request.partnerId(), request.orderId(), request.amount());

        return session;
    }

    /**
     * Get checkout session by token.
     */
    public CheckoutSession getSession(String token) {
        CheckoutSession session = sessions.get(token);
        if (session == null) {
            throw new IllegalArgumentException("Checkout session not found: " + token);
        }
        return session;
    }

    /**
     * Update checkout session when payment method is selected and payment completes.
     */
    public CheckoutSession completeCheckout(String token, String paymentMethod, String paymentReference) {
        CheckoutSession session = sessions.get(token);
        if (session == null) {
            throw new IllegalArgumentException("Checkout session not found: " + token);
        }
        if (!"PENDING".equals(session.status())) {
            throw new IllegalStateException("Checkout session is not pending: " + session.status());
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Checkout session has expired");
        }

        CheckoutSession completed = new CheckoutSession(
                session.token(), session.partnerId(), session.orderId(),
                session.amount(), session.currency(), session.itemName(),
                session.customerName(), session.customerEmail(),
                session.callbackUrl(), session.redirectUrl(),
                "PAID", paymentMethod, paymentReference,
                session.createdAt(), session.expiresAt(), session.checkoutUrl()
        );

        sessions.put(token, completed);
        Log.infof("Checkout %s completed with method=%s ref=%s", token, paymentMethod, paymentReference);

        return completed;
    }

    private String generateCheckoutUrl(String token) {
        return "https://checkout.payu.fajjjar.my.id/pay/" + token;
    }

    /**
     * Checkout creation request.
     */
    public record CreateCheckoutRequest(
            String partnerId,
            String orderId,
            BigDecimal amount,
            String currency,
            String itemName,
            String customerName,
            String customerEmail,
            String callbackUrl,
            String redirectUrl
    ) {}

    /**
     * Checkout session details.
     */
    public record CheckoutSession(
            String token,
            String partnerId,
            String orderId,
            BigDecimal amount,
            String currency,
            String itemName,
            String customerName,
            String customerEmail,
            String callbackUrl,
            String redirectUrl,
            String status,
            String paymentMethod,
            String paymentReference,
            Instant createdAt,
            Instant expiresAt,
            String checkoutUrl
    ) {}
}
