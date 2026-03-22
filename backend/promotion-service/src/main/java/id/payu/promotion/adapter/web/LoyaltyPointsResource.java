package id.payu.promotion.adapter.web;

import id.payu.promotion.domain.LoyaltyPoints;
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

    @PostMapping
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
            LoyaltyPoints loyaltyPoints = loyaltyPointsService.addPoints(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(LoyaltyPointsResponse.from(loyaltyPoints));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/redeem")
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
            LoyaltyPoints loyaltyPoints = loyaltyPointsService.redeemPoints(request);
            return ResponseEntity.ok(LoyaltyPointsResponse.from(loyaltyPoints));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
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
        Optional<LoyaltyPoints> loyaltyPointsOpt = loyaltyPointsService.getLoyaltyPoints(id);
        if (loyaltyPointsOpt.isPresent()) {
            return ResponseEntity.ok(LoyaltyPointsResponse.from(loyaltyPointsOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Loyalty points record not found"));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get loyalty points by account", description = "Retrieve all loyalty points for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loyalty points retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<LoyaltyPointsResponse>> getLoyaltyPointsByAccount(@PathVariable String accountId) {
        List<LoyaltyPoints> loyaltyPoints = loyaltyPointsService.getLoyaltyPointsByAccount(accountId);
        return ResponseEntity.ok(loyaltyPoints.stream().map(LoyaltyPointsResponse::from).toList());
    }

    @GetMapping("/account/{accountId}/balance")
    @Operation(summary = "Get loyalty points balance", description = "Retrieve loyalty points balance for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<LoyaltyBalanceResponse> getBalance(@PathVariable String accountId) {
        LoyaltyBalanceResponse balance = loyaltyPointsService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    record ErrorResponse(String message) {}
}
