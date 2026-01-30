package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.wallet.application.service.PocketNotFoundException;
import id.payu.wallet.dto.CreatePocketRequest;
import id.payu.wallet.dto.PocketResponse;
import id.payu.wallet.dto.PocketTransactionRequest;
import id.payu.wallet.dto.TotalBalanceResponse;
import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.port.in.PocketUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for pocket operations.
 * Driving adapter in Hexagonal Architecture.
 */
@RestController
@RequestMapping("/wallet-api/v1/pockets")
@Tag(name = "Pocket", description = "Pocket (sub-wallet) management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PocketController extends BaseController {

    private final PocketUseCase pocketUseCase;

    public PocketController(PocketUseCase pocketUseCase) {
        this.pocketUseCase = pocketUseCase;
    }

    @PostMapping
    @Operation(summary = "Create pocket", description = "Creates a new pocket for the authenticated user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pocket created successfully",
            content = @Content(schema = @Schema(implementation = PocketResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<ApiResponse<PocketResponse>> createPocket(
            @Valid @RequestBody CreatePocketRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        Pocket pocket = pocketUseCase.createPocket(
                accountId,
                request.getName(),
                request.getDescription(),
                request.getCurrency());

        return ok(toResponse(pocket));
    }

    @GetMapping("/{pocketId}")
    @Operation(summary = "Get pocket", description = "Retrieves a specific pocket by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pocket found",
            content = @Content(schema = @Schema(implementation = PocketResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pocket not found")
    public ResponseEntity<ApiResponse<PocketResponse>> getPocket(
            @Parameter(description = "Pocket ID", required = true) @PathVariable UUID pocketId) {
        Pocket pocket = pocketUseCase.getPocketById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));
        return ok(toResponse(pocket));
    }

    @GetMapping
    @Operation(summary = "Get all pockets", description = "Retrieves all pockets for the authenticated user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pockets retrieved successfully",
            content = @Content(schema = @Schema(implementation = PocketResponse.class)))
    public ResponseEntity<ApiResponse<List<PocketResponse>>> getPockets(
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        List<Pocket> pockets = pocketUseCase.getPocketsByAccountId(accountId);

        List<PocketResponse> responses = pockets.stream()
                .map(this::toResponse)
                .toList();

        return ok(responses);
    }

    @GetMapping("/currency/{currency}")
    @Operation(summary = "Get pockets by currency", description = "Retrieves pockets filtered by currency")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pockets retrieved successfully",
            content = @Content(schema = @Schema(implementation = PocketResponse.class)))
    public ResponseEntity<ApiResponse<List<PocketResponse>>> getPocketsByCurrency(
            @Parameter(description = "Currency code", required = true) @PathVariable String currency,
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        List<Pocket> pockets = pocketUseCase.getPocketsByAccountIdAndCurrency(accountId, currency);

        List<PocketResponse> responses = pockets.stream()
                .map(this::toResponse)
                .toList();

        return ok(responses);
    }

    @PostMapping("/{pocketId}/credit")
    @Idempotent(required = true)
    @Operation(summary = "Credit pocket", description = "Credits amount to a pocket")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Amount credited successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pocket not found")
    public ResponseEntity<ApiResponse<Void>> creditPocket(
            @Parameter(description = "Pocket ID", required = true) @PathVariable UUID pocketId,
            @Valid @RequestBody PocketTransactionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        pocketUseCase.creditPocket(pocketId, request.getAmount(), request.getReferenceId());
        return ok(null);
    }

    @PostMapping("/{pocketId}/debit")
    @Idempotent(required = true)
    @Operation(summary = "Debit pocket", description = "Debits amount from a pocket")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Amount debited successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pocket not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient balance")
    public ResponseEntity<ApiResponse<Void>> debitPocket(
            @Parameter(description = "Pocket ID", required = true) @PathVariable UUID pocketId,
            @Valid @RequestBody PocketTransactionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        pocketUseCase.debitPocket(pocketId, request.getAmount(), request.getReferenceId());
        return ok(null);
    }

    @PostMapping("/{pocketId}/freeze")
    @Operation(summary = "Freeze pocket", description = "Freezes a pocket to prevent transactions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pocket frozen successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pocket not found")
    public ResponseEntity<ApiResponse<Void>> freezePocket(
            @Parameter(description = "Pocket ID", required = true) @PathVariable UUID pocketId) {
        pocketUseCase.freezePocket(pocketId);
        return ok(null);
    }

    @PostMapping("/{pocketId}/unfreeze")
    @Operation(summary = "Unfreeze pocket", description = "Unfreezes a pocket to allow transactions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pocket unfrozen successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pocket not found")
    public ResponseEntity<ApiResponse<Void>> unfreezePocket(
            @Parameter(description = "Pocket ID", required = true) @PathVariable UUID pocketId) {
        pocketUseCase.unfreezePocket(pocketId);
        return ok(null);
    }

    @PostMapping("/{pocketId}/close")
    @Operation(summary = "Close pocket", description = "Closes a pocket")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pocket closed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pocket not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Pocket has remaining balance")
    public ResponseEntity<ApiResponse<Void>> closePocket(
            @Parameter(description = "Pocket ID", required = true) @PathVariable UUID pocketId) {
        pocketUseCase.closePocket(pocketId);
        return ok(null);
    }

    @GetMapping("/total-balance/{targetCurrency}")
    @Operation(summary = "Get total balance", description = "Calculates total balance across all pockets in target currency")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Total balance calculated successfully",
            content = @Content(schema = @Schema(implementation = TotalBalanceResponse.class)))
    public ResponseEntity<ApiResponse<TotalBalanceResponse>> getTotalBalance(
            @Parameter(description = "Target currency code", required = true) @PathVariable String targetCurrency,
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        var totalBalance = pocketUseCase.getTotalBalanceInCurrency(accountId, targetCurrency);

        TotalBalanceResponse response = new TotalBalanceResponse();
        response.setAccountId(accountId);
        response.setTargetCurrency(targetCurrency);
        response.setTotalBalance(totalBalance);

        return ok(response);
    }

    private PocketResponse toResponse(Pocket pocket) {
        PocketResponse response = new PocketResponse();
        response.setId(pocket.getId());
        response.setAccountId(pocket.getAccountId());
        response.setName(pocket.getName());
        response.setDescription(pocket.getDescription());
        response.setCurrency(pocket.getCurrency());
        response.setBalance(pocket.getBalance());
        response.setStatus(pocket.getStatus().name());
        response.setCreatedAt(pocket.getCreatedAt());
        response.setUpdatedAt(pocket.getUpdatedAt());
        return response;
    }
}
