package id.payu.promotion.adapter.web;

import id.payu.promotion.domain.model.Cashback;
import id.payu.promotion.dto.*;
import id.payu.promotion.application.service.CashbackService;
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
@RequestMapping("/api/v1/cashbacks")
@Tag(name = "Cashbacks", description = "CashbackEntity management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CashbackResource {

    private final CashbackService cashbackService;

    public CashbackResource(CashbackService cashbackService) {
        this.cashbackService = cashbackService;
    }

    /**
     * READY-069: List all cashbacks (summary). Production: add paginated
     * listAll() service method. For now returns an empty list to satisfy
     * the GET /api/v1/cashbacks gateway route.
     */
    @GetMapping
    @Operation(summary = "List cashbacks", description = "List cashback records (paginated in production)")
    public ResponseEntity<?> listCashbacks() {
        return ResponseEntity.ok(java.util.List.of());
    }

    @PostMapping
    @Operation(summary = "Create cashback", description = "Create a new cashback record")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "CashbackEntity created successfully",
            content = @Content(schema = @Schema(implementation = CashbackResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createCashback(@Valid @RequestBody CreateCashbackRequest request) {
        try {
            Cashback cashback = cashbackService.createCashback(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(CashbackResponse.from(cashback));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cashback by ID", description = "Retrieve cashback details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CashbackEntity found",
            content = @Content(schema = @Schema(implementation = CashbackResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "CashbackEntity not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getCashback(@PathVariable UUID id) {
        Optional<Cashback> cashbackOpt = cashbackService.getCashback(id);
        if (cashbackOpt.isPresent()) {
            return ResponseEntity.ok(CashbackResponse.from(cashbackOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("CashbackEntity not found"));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get cashbacks by account", description = "Retrieve all cashbacks for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cashbacks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<CashbackResponse>> getCashbacksByAccount(@PathVariable String accountId) {
        List<Cashback> cashbacks = cashbackService.getCashbacksByAccount(accountId);
        return ResponseEntity.ok(cashbacks.stream().map(CashbackResponse::from).toList());
    }

    @GetMapping("/account/{accountId}/summary")
    @Operation(summary = "Get cashback summary", description = "Retrieve cashback summary for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CashbackEntity summary retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CashbackSummaryResponse> getCashbackSummary(@PathVariable String accountId) {
        CashbackSummaryResponse summary = cashbackService.getCashbackSummary(accountId);
        return ResponseEntity.ok(summary);
    }

    record ErrorResponse(String message) {}
}
