package id.payu.promotion.resource;

import id.payu.promotion.domain.Cashback;
import id.payu.promotion.dto.*;
import id.payu.promotion.service.CashbackService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cashbacks")
@Tag(name = "Cashbacks", description = "Cashback management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CashbackResource {

    private final CashbackService cashbackService;

    public CashbackResource(CashbackService cashbackService) {
        this.cashbackService = cashbackService;
    }

    @PostMapping
    @Operation(summary = "Create cashback", description = "Create a new cashback record")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cashback created successfully",
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
        @ApiResponse(responseCode = "200", description = "Cashback found",
            content = @Content(schema = @Schema(implementation = CashbackResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Cashback not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getCashback(@PathVariable UUID id) {
        return cashbackService.getCashback(id)
            .map(cashback -> ResponseEntity.ok(CashbackResponse.from(cashback)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Cashback not found")));
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
        @ApiResponse(responseCode = "200", description = "Cashback summary retrieved successfully"),
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
