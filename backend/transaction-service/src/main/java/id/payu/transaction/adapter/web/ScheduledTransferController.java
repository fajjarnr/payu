package id.payu.transaction.adapter.web;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.domain.port.in.ScheduledTransferUseCase;
import id.payu.transaction.dto.CreateScheduledTransferRequest;
import id.payu.transaction.dto.ScheduledTransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/scheduled-transfers")
@RequiredArgsConstructor
public class ScheduledTransferController {

    private final ScheduledTransferUseCase scheduledTransferUseCase;

    @PostMapping
    public ResponseEntity<ScheduledTransferResponse> createScheduledTransfer(
            @Valid @RequestBody CreateScheduledTransferRequest request) {
        ScheduledTransferResponse response = scheduledTransferUseCase.createScheduledTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTransferResponse> getScheduledTransfer(@PathVariable UUID id) {
        ScheduledTransferResponse response = scheduledTransferUseCase.getScheduledTransfer(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<ScheduledTransfer>> getAccountScheduledTransfers(
            @PathVariable UUID accountId) {
        List<ScheduledTransfer> transfers = scheduledTransferUseCase.getAccountScheduledTransfers(accountId);
        return ResponseEntity.ok(transfers);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledTransferResponse> updateScheduledTransfer(
            @PathVariable UUID id,
            @Valid @RequestBody CreateScheduledTransferRequest request) {
        ScheduledTransferResponse response = scheduledTransferUseCase.updateScheduledTransfer(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelScheduledTransfer(@PathVariable UUID id) {
        scheduledTransferUseCase.cancelScheduledTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pauseScheduledTransfer(@PathVariable UUID id) {
        scheduledTransferUseCase.pauseScheduledTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Void> resumeScheduledTransfer(@PathVariable UUID id) {
        scheduledTransferUseCase.resumeScheduledTransfer(id);
        return ResponseEntity.noContent().build();
    }
}
