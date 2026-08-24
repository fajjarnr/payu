package id.payu.dispute.adapter.web;

import id.payu.dispute.domain.model.Chargeback;
import id.payu.dispute.domain.port.in.ChargebackUseCase;
import id.payu.dispute.interfaces.dto.ChargebackResponse;
import id.payu.dispute.interfaces.dto.CreateChargebackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/chargebacks")
@RequiredArgsConstructor
public class ChargebackController {

    private final ChargebackUseCase chargebackUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ChargebackResponse> create(@Valid @RequestBody CreateChargebackRequest req) {
        Chargeback cb = chargebackUseCase.create(req.getTransactionId(), req.getCustomerId(), req.getMerchantId(), req.getAmount(), req.getCurrency(), req.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChargebackResponse.from(cb));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ChargebackResponse> submit(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String schemeCaseId = body.get("schemeCaseId");
        Chargeback cb = chargebackUseCase.submit(id, schemeCaseId);
        return ResponseEntity.ok(ChargebackResponse.from(cb));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ChargebackResponse> startReview(@PathVariable UUID id) {
        Chargeback cb = chargebackUseCase.startReview(id);
        return ResponseEntity.ok(ChargebackResponse.from(cb));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ChargebackResponse> accept(@PathVariable UUID id) {
        Chargeback cb = chargebackUseCase.accept(id);
        return ResponseEntity.ok(ChargebackResponse.from(cb));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ChargebackResponse> reject(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String reason = body.get("rejectionReason");
        Chargeback cb = chargebackUseCase.reject(id, reason);
        return ResponseEntity.ok(ChargebackResponse.from(cb));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ChargebackResponse> reverse(@PathVariable UUID id) {
        Chargeback cb = chargebackUseCase.reverse(id);
        return ResponseEntity.ok(ChargebackResponse.from(cb));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ChargebackResponse> close(@PathVariable UUID id) {
        Chargeback cb = chargebackUseCase.close(id);
        return ResponseEntity.ok(ChargebackResponse.from(cb));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ChargebackResponse> getById(@PathVariable UUID id) {
        return chargebackUseCase.getById(id)
                .map(cb -> ResponseEntity.ok(ChargebackResponse.from(cb)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<ChargebackResponse>> getAll(@RequestParam(required = false) String status) {
        List<Chargeback> list;
        if (status != null) {
            list = chargebackUseCase.getByStatus(status);
        } else {
            list = chargebackUseCase.getAll();
        }
        return ResponseEntity.ok(list.stream().map(ChargebackResponse::from).collect(Collectors.toList()));
    }
}
