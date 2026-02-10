package id.payu.promotion.adapter.web;

import id.payu.promotion.domain.Promotion;
import id.payu.promotion.dto.CreatePromotionRequest;
import id.payu.promotion.dto.UpdatePromotionRequest;
import id.payu.promotion.dto.ClaimPromotionRequest;
import id.payu.promotion.dto.PromotionResponse;
import id.payu.promotion.dto.RewardResponse;
import id.payu.promotion.application.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/promotions")
@Tag(name = "Promotions", description = "Promotion management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PromotionResource extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(PromotionResource.class);

    private final PromotionService promotionService;

    public PromotionResource(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    @Operation(summary = "Create promotion", description = "Create a new promotion campaign")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Promotion created successfully",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createPromotion(@Valid @RequestBody CreatePromotionRequest request) {
        try {
            Promotion promotion = promotionService.createPromotion(request);
            return created(PromotionResponse.from(promotion), "/api/v1/promotions/{id}", promotion.getId());
        } catch (IllegalArgumentException e) {
            return badRequest("PROMO_001", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update promotion", description = "Update an existing promotion")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Promotion updated successfully",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Promotion not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updatePromotion(@PathVariable UUID id, @RequestBody UpdatePromotionRequest request) {
        try {
            Promotion promotion = promotionService.updatePromotion(id, request);
            return ok(PromotionResponse.from(promotion));
        } catch (IllegalArgumentException e) {
            return badRequest("PROMO_002", e.getMessage());
        }
    }

    @PostMapping("/{code}/claim")
    @Operation(summary = "Claim promotion", description = "Claim a promotion using a promo code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Promotion claimed successfully",
            content = @Content(schema = @Schema(implementation = RewardResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or promotion not claimable"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> claimPromotion(@PathVariable String code, @Valid @RequestBody ClaimPromotionRequest request) {
        try {
            id.payu.promotion.domain.Reward reward = promotionService.claimPromotion(code, request);
            return created(RewardResponse.from(reward), "/api/v1/promotions/rewards/{id}", reward.getId());
        } catch (IllegalArgumentException e) {
            return badRequest("PROMO_003", e.getMessage());
        }
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate promotion", description = "Activate a promotion campaign")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Promotion activated successfully",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Promotion not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> activatePromotion(@PathVariable UUID id) {
        try {
            Promotion promotion = promotionService.activatePromotion(id);
            return ok(PromotionResponse.from(promotion));
        } catch (IllegalArgumentException e) {
            return badRequest("PROMO_004", e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get promotion by ID", description = "Retrieve promotion details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Promotion found",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Promotion not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getPromotion(@PathVariable UUID id) {
        Optional<Promotion> promotionOpt = promotionService.getPromotion(id);
        if (promotionOpt.isPresent()) {
            return ok(PromotionResponse.from(promotionOpt.get()));
        }
        return notFound("PROMO_404", "Promotion not found");
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get promotion by code", description = "Retrieve promotion details by promo code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Promotion found",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Promotion not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getPromotionByCode(@PathVariable String code) {
        Optional<Promotion> promotionOpt = promotionService.getPromotionByCode(code);
        if (promotionOpt.isPresent()) {
            return ok(PromotionResponse.from(promotionOpt.get()));
        }
        return notFound("PROMO_404", "Promotion not found");
    }

    @GetMapping
    @Operation(summary = "Get active promotions", description = "Retrieve all currently active promotions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active promotions retrieved successfully",
            content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getActivePromotions() {
        List<Promotion> promotions = promotionService.getActivePromotions();
        return ok(promotions.stream().map(PromotionResponse::from).toList());
    }
}
