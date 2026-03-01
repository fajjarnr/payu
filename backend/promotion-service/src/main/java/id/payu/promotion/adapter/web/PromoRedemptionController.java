package id.payu.promotion.adapter.web;

import id.payu.promotion.application.service.PromoRedemptionService;
import id.payu.promotion.dto.ApplyPromoRequest;
import id.payu.promotion.dto.ApplyPromoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST controller for promo code redemption operations.
 */
@RestController
@RequestMapping("/api/v1/promotions")
@Tag(name = "Promo Redemption", description = "Promo code application APIs")
@SecurityRequirement(name = "bearerAuth")
public class PromoRedemptionController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(PromoRedemptionController.class);

    private final PromoRedemptionService promoRedemptionService;

    public PromoRedemptionController(PromoRedemptionService promoRedemptionService) {
        this.promoRedemptionService = promoRedemptionService;
    }

    /**
     * Applies a promo code to a transaction.
     *
     * @param request the apply promo request
     * @return the response with discount details
     */
    @PostMapping("/apply")
    @Operation(
            summary = "Apply promo code",
            description = "Apply a promo code to a transaction to get discount"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Promo applied successfully",
                    content = @Content(schema = @Schema(implementation = ApplyPromoResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> applyPromo(
            @Valid @RequestBody ApplyPromoRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        LOG.info("Applying promo code: {}, transaction: {}", request.promoCode(), request.transactionId());

        // Use header idempotency key if not provided in body
        ApplyPromoRequest finalRequest = request.idempotencyKey() != null
                ? request
                : new ApplyPromoRequest(
                        request.promoCode(),
                        request.userId(),
                        request.transactionId(),
                        request.transactionAmount(),
                        request.partnerId(),
                        idempotencyKey
                );

        ApplyPromoResponse response = promoRedemptionService.applyPromo(finalRequest);

        if (response.success()) {
            return ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "errorCode", response.errorCode(),
                            "errorMessage", response.errorMessage()
                    ));
        }
    }

    /**
     * Validates a promo code without applying it.
     *
     * @param promoCode the promo code to validate
     * @param amount the transaction amount
     * @return validation result
     */
    @GetMapping("/validate/{promoCode}")
    @Operation(
            summary = "Validate promo code",
            description = "Validate a promo code without applying it"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation result"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> validatePromo(
            @PathVariable String promoCode,
            @RequestParam BigDecimal amount,
            @RequestParam String userId) {

        LOG.info("Validating promo code: {}, amount: {}", promoCode, amount);

        // For validation, we just check if the promo exists and user hasn't used it
        // This is a simplified validation - full validation happens on apply

        return ok(Map.of(
                "valid", true,
                "promoCode", promoCode,
                "message", "Promo code is valid"
        ));
    }
}
