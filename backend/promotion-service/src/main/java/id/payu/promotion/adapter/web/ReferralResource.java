package id.payu.promotion.adapter.web;

import id.payu.promotion.domain.model.Referral;
import id.payu.promotion.interfaces.dto.*;
import id.payu.promotion.application.service.ReferralService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@Tag(name = "Referrals", description = "ReferralEntity management APIs")
@SecurityRequirement(name = "bearerAuth")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class ReferralResource {

    private final ReferralService referralService;

    public ReferralResource(ReferralService referralService) {
        this.referralService = referralService;
    }

    /**
     * READY-069: List all referrals. Returns empty list — add paginated
     * listAll() in production.
     */
    @GetMapping
    @Operation(summary = "List referrals", description = "List referral records (paginated in production)")
    public ResponseEntity<?> listReferrals() {
        return ResponseEntity.ok(java.util.List.of());
    }

    @PostMapping
    @Operation(summary = "Create referral", description = "Create a new referral")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "ReferralEntity created successfully",
            content = @Content(schema = @Schema(implementation = ReferralResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createReferral(@Valid @RequestBody CreateReferralRequest request) {
        try {
            Referral referral = referralService.createReferral(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReferralResponse.from(referral));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/complete")
    @Operation(summary = "Complete referral", description = "Complete a referral after conditions are met")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ReferralEntity completed successfully",
            content = @Content(schema = @Schema(implementation = ReferralResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> completeReferral(@Valid @RequestBody CompleteReferralRequest request) {
        try {
            Referral referral = referralService.completeReferral(request);
            return ResponseEntity.ok(ReferralResponse.from(referral));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get referral by ID", description = "Retrieve referral details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ReferralEntity found",
            content = @Content(schema = @Schema(implementation = ReferralResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "ReferralEntity not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getReferral(@PathVariable UUID id) {
        Optional<Referral> referralOpt = referralService.getReferral(id);
        if (referralOpt.isPresent()) {
            return ResponseEntity.ok(ReferralResponse.from(referralOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("ReferralEntity not found"));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get referral by code", description = "Retrieve referral details by referral code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ReferralEntity found",
            content = @Content(schema = @Schema(implementation = ReferralResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "ReferralEntity code not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getReferralByCode(@PathVariable String code) {
        Optional<Referral> referralOpt = referralService.getReferralByCode(code);
        if (referralOpt.isPresent()) {
            return ResponseEntity.ok(ReferralResponse.from(referralOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("ReferralEntity code not found"));
    }

    @GetMapping("/referrer/{referrerAccountId}")
    @Operation(summary = "Get referrals by referrer", description = "Retrieve all referrals for a referrer account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Referrals retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ReferralResponse>> getReferralsByReferrer(@PathVariable String referrerAccountId) {
        List<Referral> referrals = referralService.getReferralsByReferrer(referrerAccountId);
        return ResponseEntity.ok(referrals.stream().map(ReferralResponse::from).toList());
    }

    @GetMapping("/referrer/{referrerAccountId}/summary")
    @Operation(summary = "Get referral summary", description = "Retrieve referral summary for a referrer account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ReferralEntity summary retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ReferralSummaryResponse> getReferralSummary(@PathVariable String referrerAccountId) {
        ReferralSummaryResponse summary = referralService.getReferralSummary(referrerAccountId);
        return ResponseEntity.ok(summary);
    }

    record ErrorResponse(String message) {}
}
