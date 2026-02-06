package id.payu.transaction.adapter.web;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.domain.port.in.ScheduledTransferUseCase;
import id.payu.transaction.dto.CreateScheduledTransferRequest;
import id.payu.transaction.dto.ScheduledTransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Scheduled Transfers", description = "Scheduled and recurring transfer APIs")
@SecurityRequirement(name = "bearerAuth")
public class ScheduledTransferController {

    private final ScheduledTransferUseCase scheduledTransferUseCase;

    @PostMapping
    @Operation(
            summary = "Create scheduled transfer",
            description = """
                    Creates a new scheduled or recurring transfer.

                    **Supported Schedules:**
                    - ONE_TIME: Single future transfer
                    - DAILY: Daily transfers
                    - WEEKLY: Weekly transfers
                    - MONTHLY: Monthly transfers
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Scheduled transfer created successfully",
                    content = @Content(schema = @Schema(implementation = ScheduledTransferResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<ScheduledTransferResponse> createScheduledTransfer(
            @Parameter(description = "Scheduled transfer request", required = true)
            @Valid @RequestBody CreateScheduledTransferRequest request) {
        ScheduledTransferResponse response = scheduledTransferUseCase.createScheduledTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get scheduled transfer",
            description = "Retrieves details of a specific scheduled transfer by ID."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Scheduled transfer found",
                    content = @Content(schema = @Schema(implementation = ScheduledTransferResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<ScheduledTransferResponse> getScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id) {
        ScheduledTransferResponse response = scheduledTransferUseCase.getScheduledTransfer(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}")
    @Operation(
            summary = "List account scheduled transfers",
            description = "Retrieves all scheduled transfers for a specific account."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of scheduled transfers"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<List<ScheduledTransfer>> getAccountScheduledTransfers(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId) {
        List<ScheduledTransfer> transfers = scheduledTransferUseCase.getAccountScheduledTransfers(accountId);
        return ResponseEntity.ok(transfers);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update scheduled transfer",
            description = """
                    Modifies an existing scheduled transfer.

                    **Note:** Only transfers with PENDING or PAUSED status can be modified.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Scheduled transfer updated successfully",
                    content = @Content(schema = @Schema(implementation = ScheduledTransferResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request | Transfer already processed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<ScheduledTransferResponse> updateScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Updated scheduled transfer details", required = true)
            @Valid @RequestBody CreateScheduledTransferRequest request) {
        ScheduledTransferResponse response = scheduledTransferUseCase.updateScheduledTransfer(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel scheduled transfer",
            description = """
                    Cancels a scheduled transfer permanently.

                    **Note:** Only transfers with PENDING or PAUSED status can be cancelled.
                    Transfers that are already being processed cannot be cancelled.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Scheduled transfer cancelled successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Transfer already processed or completed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<Void> cancelScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id) {
        scheduledTransferUseCase.cancelScheduledTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    @Operation(
            summary = "Pause scheduled transfer",
            description = """
                    Temporarily pauses a scheduled transfer.

                    Paused transfers can be resumed later. This is useful for
                    temporarily stopping recurring transfers without deleting them.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Scheduled transfer paused successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Transfer already paused or completed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<Void> pauseScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id) {
        scheduledTransferUseCase.pauseScheduledTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resume")
    @Operation(
            summary = "Resume paused transfer",
            description = """
                    Resumes a previously paused scheduled transfer.

                    Only transfers with PAUSED status can be resumed.
                    The next execution will be scheduled according to the original frequency.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Scheduled transfer resumed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Transfer is not paused"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<Void> resumeScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id) {
        scheduledTransferUseCase.resumeScheduledTransfer(id);
        return ResponseEntity.noContent().build();
    }
}
