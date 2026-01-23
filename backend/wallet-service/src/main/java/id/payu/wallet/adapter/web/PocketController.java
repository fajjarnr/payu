package id.payu.wallet.adapter.web;

import id.payu.wallet.application.service.PocketNotFoundException;
import id.payu.wallet.dto.CreatePocketRequest;
import id.payu.wallet.dto.PocketResponse;
import id.payu.wallet.dto.PocketTransactionRequest;
import id.payu.wallet.dto.TotalBalanceResponse;
import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.port.in.PocketUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wallet-api/v1/pockets")
public class PocketController {

    private final PocketUseCase pocketUseCase;

    public PocketController(PocketUseCase pocketUseCase) {
        this.pocketUseCase = pocketUseCase;
    }

    @PostMapping
    public ResponseEntity<PocketResponse> createPocket(
            @Valid @RequestBody CreatePocketRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        Pocket pocket = pocketUseCase.createPocket(
                accountId,
                request.getName(),
                request.getDescription(),
                request.getCurrency());

        return ResponseEntity.ok(toResponse(pocket));
    }

    @GetMapping("/{pocketId}")
    public ResponseEntity<PocketResponse> getPocket(@PathVariable UUID pocketId) {
        Pocket pocket = pocketUseCase.getPocketById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException(pocketId.toString()));
        return ResponseEntity.ok(toResponse(pocket));
    }

    @GetMapping
    public ResponseEntity<List<PocketResponse>> getPockets(
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        List<Pocket> pockets = pocketUseCase.getPocketsByAccountId(accountId);

        List<PocketResponse> responses = pockets.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/currency/{currency}")
    public ResponseEntity<List<PocketResponse>> getPocketsByCurrency(
            @PathVariable String currency,
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        List<Pocket> pockets = pocketUseCase.getPocketsByAccountIdAndCurrency(accountId, currency);

        List<PocketResponse> responses = pockets.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{pocketId}/credit")
    public ResponseEntity<Void> creditPocket(
            @PathVariable UUID pocketId,
            @Valid @RequestBody PocketTransactionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        pocketUseCase.creditPocket(pocketId, request.getAmount(), request.getReferenceId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{pocketId}/debit")
    public ResponseEntity<Void> debitPocket(
            @PathVariable UUID pocketId,
            @Valid @RequestBody PocketTransactionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        pocketUseCase.debitPocket(pocketId, request.getAmount(), request.getReferenceId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{pocketId}/freeze")
    public ResponseEntity<Void> freezePocket(@PathVariable UUID pocketId) {
        pocketUseCase.freezePocket(pocketId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{pocketId}/unfreeze")
    public ResponseEntity<Void> unfreezePocket(@PathVariable UUID pocketId) {
        pocketUseCase.unfreezePocket(pocketId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{pocketId}/close")
    public ResponseEntity<Void> closePocket(@PathVariable UUID pocketId) {
        pocketUseCase.closePocket(pocketId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/total-balance/{targetCurrency}")
    public ResponseEntity<TotalBalanceResponse> getTotalBalance(
            @PathVariable String targetCurrency,
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        var totalBalance = pocketUseCase.getTotalBalanceInCurrency(accountId, targetCurrency);

        TotalBalanceResponse response = new TotalBalanceResponse();
        response.setAccountId(accountId);
        response.setTargetCurrency(targetCurrency);
        response.setTotalBalance(totalBalance);

        return ResponseEntity.ok(response);
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
