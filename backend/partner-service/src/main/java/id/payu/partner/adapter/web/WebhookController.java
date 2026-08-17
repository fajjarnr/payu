package id.payu.partner.adapter.web;

import id.payu.partner.application.service.WebhookService;
import id.payu.partner.interfaces.dto.WebhookSubscriptionDTO;
import id.payu.partner.interfaces.dto.WebhookDeliveryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;

import java.util.List;
import id.payu.security.annotation.AuditOperation;

/**
 * REST controller for managing outbound webhook subscriptions.
 * Partners can register URLs to receive real-time event notifications
 * via HTTP POST with HMAC-SHA256 signature verification.
 *
 * <p>All endpoints are scoped to a specific partner and require ADMIN role.</p>
 */
@RestController
@RequestMapping({"/v1/partners/{partnerId}/webhooks", "/partners/{partnerId}/webhooks"})
@Tag(name = "Webhooks", description = "Outbound webhook subscription management")
@PreAuthorize("hasRole('ADMIN')")
public class WebhookController extends BaseController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    @Operation(
        summary = "Register a webhook subscription",
        description = "Register a new webhook URL for the partner. "
            + "A HMAC-SHA256 secret will be generated and returned (only shown once at creation)."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Webhook subscription created",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "PartnerEntity not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Webhook URL already registered for this partner",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = AuditOperation.CREATE, entityType = "WebhookSubscriptionEntity", level = AuditLevel.INFO)
    public ResponseEntity<?> createWebhook(
            @Parameter(description = "PartnerEntity ID", required = true)
            @PathVariable Long partnerId,
            @Valid @RequestBody WebhookSubscriptionDTO dto) {
        return created(webhookService.createSubscription(partnerId, dto));
    }

    @GetMapping
    @Operation(
        summary = "List webhook subscriptions",
        description = "Retrieves all webhook subscriptions for the specified partner."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Webhook subscriptions retrieved",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "PartnerEntity not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> listWebhooks(
            @Parameter(description = "PartnerEntity ID", required = true)
            @PathVariable Long partnerId) {
        return ok(webhookService.listSubscriptions(partnerId));
    }

    @GetMapping("/{webhookId}")
    @Operation(
        summary = "Get webhook subscription",
        description = "Retrieves details of a specific webhook subscription."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Webhook subscription found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Webhook or partner not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> getWebhook(
            @Parameter(description = "PartnerEntity ID", required = true)
            @PathVariable Long partnerId,
            @Parameter(description = "Webhook subscription ID", required = true)
            @PathVariable Long webhookId) {
        return ok(webhookService.getSubscription(partnerId, webhookId));
    }

    @PutMapping("/{webhookId}")
    @Operation(
        summary = "Update webhook subscription",
        description = "Updates an existing webhook subscription. "
            + "Only the provided fields will be updated (partial update)."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Webhook subscription updated",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Webhook or partner not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = AuditOperation.UPDATE, entityType = "WebhookSubscriptionEntity", level = AuditLevel.INFO)
    public ResponseEntity<?> updateWebhook(
            @Parameter(description = "PartnerEntity ID", required = true)
            @PathVariable Long partnerId,
            @Parameter(description = "Webhook subscription ID", required = true)
            @PathVariable Long webhookId,
            @Valid @RequestBody WebhookSubscriptionDTO dto) {
        return ok(webhookService.updateSubscription(partnerId, webhookId, dto));
    }

    @DeleteMapping("/{webhookId}")
    @Operation(
        summary = "Delete webhook subscription",
        description = "Permanently removes a webhook subscription and all its delivery history."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Webhook subscription deleted"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Webhook or partner not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = AuditOperation.DELETE, entityType = "WebhookSubscriptionEntity", level = AuditLevel.WARN)
    public ResponseEntity<?> deleteWebhook(
            @Parameter(description = "PartnerEntity ID", required = true)
            @PathVariable Long partnerId,
            @Parameter(description = "Webhook subscription ID", required = true)
            @PathVariable Long webhookId) {
        webhookService.deleteSubscription(partnerId, webhookId);
        return noContent();
    }

    @PostMapping("/{webhookId}/secret/regenerate")
    @Operation(
        summary = "Regenerate webhook secret",
        description = "Generates a new HMAC-SHA256 signing secret for the webhook subscription. "
            + "The old secret becomes invalid immediately. The new secret is returned only once."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Secret regenerated",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Webhook or partner not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = AuditOperation.UPDATE, entityType = "WebhookSubscriptionEntity", maskData = true, level = AuditLevel.WARN)
    public ResponseEntity<?> regenerateSecret(
            @Parameter(description = "PartnerEntity ID", required = true)
            @PathVariable Long partnerId,
            @Parameter(description = "Webhook subscription ID", required = true)
            @PathVariable Long webhookId) {
        return ok(webhookService.regenerateSecret(partnerId, webhookId));
    }

    @GetMapping("/{webhookId}/deliveries")
    @Operation(
        summary = "Get webhook delivery history",
        description = "Retrieves the delivery log for a specific webhook subscription, "
            + "including attempt count, response codes, and retry status."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Delivery history retrieved",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Webhook or partner not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> getDeliveries(
            @Parameter(description = "PartnerEntity ID", required = true)
            @PathVariable Long partnerId,
            @Parameter(description = "Webhook subscription ID", required = true)
            @PathVariable Long webhookId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ok(webhookService.getDeliveries(partnerId, webhookId, pageable));
    }
}
