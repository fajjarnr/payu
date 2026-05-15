package id.payu.promotion.adapter.web;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.dto.*;
import id.payu.promotion.application.service.LoyaltyPointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loyalty-points")
@Tag(name = "Loyalty Points", description = "Loyalty points management APIs")
@SecurityRequirement(name = "bearerAuth")
public class LoyaltyPointsResource {

    private final LoyaltyPointsService loyaltyPointsService;

    public LoyaltyPointsResource(LoyaltyPointsService loyaltyPointsService) {
        this.loyaltyPointsService = loyaltyPointsService;
    }

    /**
     * BUG-SECURITY-024 FIX: Extract accountId from JWT principal.
     * Uses 'account_id' claim with 'sub' fallback for consistency with other PayU services.
     */
    private String extractAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT authentication found");
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String accountId = jwt.getClaimAsString("account_id");
        return accountId != null ? accountId : jwt.getSubject();
    }

    /**
     * BUG-SECURITY-024 FIX: Verify the authenticated user owns the given account.
     */
    private void verifyAccountOwnership(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        String jwtAccountId = extractAccountId();
        if (!accountId.equals(jwtAccountId)) {
            throw new AccessDeniedException("Access denied: you don't own this resource");
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add loyalty points", description = "Add loyalty points to an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Loyalty points added successfully",
            content = @Content(schema = @Schema(implementation = LoyaltyPointsResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> addPoints(@Valid @RequestBody CreateLoyaltyPointsRequest request) {
        try {
            // BUG-SECURITY-024 FIX: Verify caller owns the account
            verifyAccountOwnership(request.accountId());
            LoyaltyPointsEntity loyaltyPoints = loyaltyPointsService.addPoints(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(LoyaltyPointsResponse.from(loyaltyPoints));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/redeem")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Redeem loyalty points", description = "Redeem loyalty points from an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loyalty points redeemed successfully",
            content = @Content(schema = @Schema(implementation = LoyaltyPointsResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> redeemPoints(@Valid @RequestBody RedeemLoyaltyPointsRequest request) {
        try {
            // BUG-SECURITY-024 FIX: Verify caller owns the account
            verifyAccountOwnership(request.accountId());
            LoyaltyPointsEntity loyaltyPoints = loyaltyPointsService.redeemPoints(request);
            return ResponseEntity.ok(LoyaltyPointsResponse.from(loyaltyPoints));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get loyalty points record by ID", description = "Retrieve loyalty points record by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loyalty points record found",
            content = @Content(schema = @Schema(implementation = LoyaltyPointsResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Loyalty points record not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getLoyaltyPoints(@PathVariable UUID id) {
        Optional<LoyaltyPointsEntity> loyaltyPointsOpt = loyaltyPointsService.getLoyaltyPoints(id);
        if (loyaltyPointsOpt.isPresent()) {
            try {
                // BUG-SECURITY-024 FIX: Verify ownership
                LoyaltyPointsEntity lp = loyaltyPointsOpt.get();
                verifyAccountOwnership(lp.getAccountId());
                return ResponseEntity.ok(LoyaltyPointsResponse.from(lp));
            } catch (AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Loyalty points record not found"));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get loyalty points by account", description = "Retrieve all loyalty points for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loyalty points retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getLoyaltyPointsByAccount(@PathVariable String accountId) {
        try {
            // BUG-SECURITY-024 FIX: Verify caller owns the account
            verifyAccountOwnership(accountId);
            List<LoyaltyPointsEntity> loyaltyPoints = loyaltyPointsService.getLoyaltyPointsByAccount(accountId);
            return ResponseEntity.ok(loyaltyPoints.stream().map(LoyaltyPointsResponse::from).toList());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/account/{accountId}/balance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get loyalty points balance", description = "Retrieve loyalty points balance for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getBalance(@PathVariable String accountId) {
        try {
            // BUG-SECURITY-024 FIX: Verify caller owns the account
            verifyAccountOwnership(accountId);
            LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);
            return ResponseEntity.ok(balance);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    record ErrorResponse(String message) {}
}
