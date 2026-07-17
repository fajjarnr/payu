package id.payu.billing.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.billing.application.service.SubscriptionService;
import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.SubscriptionPlan;
import id.payu.billing.domain.model.BillingInterval;
import id.payu.billing.dto.CreateSubscriptionPlanRequest;
import id.payu.billing.dto.SubscribeRequest;
import id.payu.billing.dto.SubscriptionChargeResponse;
import id.payu.billing.dto.SubscriptionPlanResponse;
import id.payu.billing.dto.SubscriptionResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import java.util.List;
import java.util.UUID;
import id.payu.security.annotation.AuditOperation;

/**
 * REST Controller for subscription and recurring billing management.
 * Supports plan creation, subscription lifecycle, and charge history.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription & recurring billing APIs")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Extract authenticated user ID from JWT token.
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════
    //  Plan Management
    // ═══════════════════════════════════════════════════════

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @Audited(operation = AuditOperation.CREATE, entityType = "SubscriptionPlan",
            maskData = true, level = AuditLevel.INFO)
    @Idempotent(required = true)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a subscription plan",
            description = "Partners create subscription plans with pricing and billing intervals")
    public ApiResponse<SubscriptionPlanResponse> createPlan(
            @Valid @RequestBody CreateSubscriptionPlanRequest request) {
        BillingInterval interval = BillingInterval.valueOf(request.billingInterval().toUpperCase());
        SubscriptionPlan plan = subscriptionService.createPlan(
                request.partnerId(), request.planName(), request.description(),
                interval, request.price(), request.currency(),
                request.trialDays(), request.gracePeriodDays());
        return ApiResponse.success(SubscriptionPlanResponse.from(plan));
    }

    @GetMapping("/plans/{planId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get subscription plan by ID")
    public ApiResponse<SubscriptionPlanResponse> getPlan(
            @Parameter(description = "Plan UUID") @PathVariable UUID planId) {
        SubscriptionPlan plan = subscriptionService.getPlan(planId);
        return ApiResponse.success(SubscriptionPlanResponse.from(plan));
    }

    @GetMapping("/plans/partner/{partnerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List subscription plans for a partner")
    public ApiResponse<List<SubscriptionPlanResponse>> getPlansByPartner(
            @Parameter(description = "Partner ID") @PathVariable String partnerId) {
        List<SubscriptionPlanResponse> plans = subscriptionService.getPlansByPartner(partnerId)
                .stream()
                .map(SubscriptionPlanResponse::from)
                .toList();
        return ApiResponse.success(plans);
    }

    @DeleteMapping("/plans/{planId}")
    @Audited(operation = AuditOperation.UPDATE, entityType = "SubscriptionPlan",
            maskData = true, level = AuditLevel.INFO)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deactivate a subscription plan",
            description = "Soft-deletes a plan by marking it inactive")
    public ApiResponse<Void> deactivatePlan(
            @Parameter(description = "Plan UUID") @PathVariable UUID planId) {
        subscriptionService.deactivatePlan(planId);
        return ApiResponse.success(null);
    }

    // ═══════════════════════════════════════════════════════
    //  Subscription Lifecycle
    // ═══════════════════════════════════════════════════════

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Audited(operation = AuditOperation.CREATE, entityType = "Subscription",
            maskData = true, level = AuditLevel.INFO)
    @Idempotent(required = true)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a subscription",
            description = "Subscribe a user account to a plan. Starts trial if plan has trial days.")
    public ApiResponse<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscribeRequest request) {
        Subscription sub = subscriptionService.subscribe(
                request.accountId(), request.planId(), request.externalReferenceId());
        return ApiResponse.success(SubscriptionResponse.from(sub));
    }

    @GetMapping("/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get subscription by ID")
    public ApiResponse<SubscriptionResponse> getSubscription(
            @Parameter(description = "Subscription UUID") @PathVariable UUID subscriptionId) {
        Subscription sub = subscriptionService.getSubscription(subscriptionId);
        // BUG-SECURITY-002 FIX: Validate authenticated user owns this subscription
        String userId = extractUserId();
        if (userId != null && sub.getAccountId() != null && !Objects.equals(sub.getAccountId(), userId)) {
            throw new IllegalArgumentException("Subscription not found");
        }
        return ApiResponse.success(SubscriptionResponse.from(sub));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List subscriptions for an account")
    public ApiResponse<List<SubscriptionResponse>> getSubscriptionsByAccount(
            @Parameter(description = "Account ID") @PathVariable String accountId) {
        // BUG-SECURITY-002 FIX: Validate authenticated user is requesting their own account
        String userId = extractUserId();
        if (!Objects.equals(accountId, userId)) {
            throw new IllegalArgumentException("Not authorized to access subscriptions for this account");
        }
        List<SubscriptionResponse> subs = subscriptionService.getSubscriptionsByAccount(accountId)
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
        return ApiResponse.success(subs);
    }

    @GetMapping("/partner/{partnerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List subscriptions for a partner")
    public ApiResponse<List<SubscriptionResponse>> getSubscriptionsByPartner(
            @Parameter(description = "Partner ID") @PathVariable String partnerId) {
        List<SubscriptionResponse> subs = subscriptionService.getSubscriptionsByPartner(partnerId)
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
        return ApiResponse.success(subs);
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Audited(operation = AuditOperation.UPDATE, entityType = "Subscription",
            maskData = true, level = AuditLevel.INFO)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a subscription",
            description = "Immediately cancels the subscription with an optional reason")
    public ApiResponse<SubscriptionResponse> cancelSubscription(
            @Parameter(description = "Subscription UUID") @PathVariable UUID subscriptionId,
            @RequestParam(required = false) String reason) {
        Subscription sub = subscriptionService.cancelSubscription(subscriptionId, reason);
        return ApiResponse.success(SubscriptionResponse.from(sub));
    }

    // ═══════════════════════════════════════════════════════
    //  Charge History
    // ═══════════════════════════════════════════════════════

    @GetMapping("/{subscriptionId}/charges")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get charge history for a subscription")
    public ApiResponse<List<SubscriptionChargeResponse>> getCharges(
            @Parameter(description = "Subscription UUID") @PathVariable UUID subscriptionId) {
        List<SubscriptionChargeResponse> charges = subscriptionService
                .getChargesBySubscription(subscriptionId)
                .stream()
                .map(SubscriptionChargeResponse::from)
                .toList();
        return ApiResponse.success(charges);
    }
}
