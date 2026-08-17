package id.payu.transaction.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;
import id.payu.transaction.domain.port.in.ScheduledTransferUseCase;
import id.payu.transaction.interfaces.dto.CreateScheduledTransferRequest;
import id.payu.transaction.interfaces.dto.ScheduledTransferResponse;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduled-transfers")
@RequiredArgsConstructor
@Tag(name = "Scheduled Transfers", description = "Scheduled and recurring transfer APIs")
@SecurityRequirement(name = "bearerAuth")
public class ScheduledTransferController {

    private final ScheduledTransferUseCase scheduledTransferUseCase;

    /**
     * Extracts the authenticated user's ID from the JWT.
     * BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback.
     * BUG-BE-148: Added to enforce ownership checks on all endpoints.
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT authentication found");
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String accountId = jwt.getClaimAsString("account_id");
        return accountId != null ? accountId : jwt.getSubject();
    }

    /**
     * Verifies the authenticated user owns the scheduled transfer.
     * Throws AccessDeniedException if ownership check fails.
     */
    private ScheduledTransferResponse verifyOwnership(UUID transferId) {
        String userId = extractUserId();
        ScheduledTransferResponse response = scheduledTransferUseCase.getScheduledTransfer(transferId);
        if (!Objects.equals(response.getSenderAccountId() == null ? null : response.getSenderAccountId().toString(), userId)) {
            throw new AccessDeniedException("Access denied: you don't own this resource");
        }
        return response;
    }

    @PostMapping
    @Idempotent(required = true)
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - sender account mismatch"
            )
    })
    public ResponseEntity<ScheduledTransferResponse> createScheduledTransfer(
            @Parameter(description = "Scheduled transfer request", required = true)
            @Valid @RequestBody CreateScheduledTransferRequest request) {
        // BUG-BE-148: Validate caller owns the sender account
        String userId = extractUserId();
        if (!Objects.equals(UUID.fromString(userId), request.getSenderAccountId())) {
            throw new AccessDeniedException("Access denied: you don't own this resource");
        }

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
                    responseCode = "403",
                    description = "Forbidden - not the owner"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<ScheduledTransferResponse> getScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id) {
        // BUG-BE-148: Verify ownership before returning
        ScheduledTransferResponse response = verifyOwnership(id);
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - account mismatch"
            )
    })
    public ResponseEntity<List<ScheduledTransferEntity>> getAccountScheduledTransfers(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId) {
        // BUG-BE-148: Verify caller owns the account
        String userId = extractUserId();
        if (!Objects.equals(accountId == null ? null : accountId.toString(), userId)) {
            throw new AccessDeniedException("Access denied: you don't own this resource");
        }

        List<ScheduledTransferEntity> transfers = scheduledTransferUseCase.getAccountScheduledTransfers(accountId);
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
                    responseCode = "403",
                    description = "Forbidden - not the owner"
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
        // BUG-BE-148: Verify ownership before updating
        verifyOwnership(id);

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
                    responseCode = "200",
                    description = "Scheduled transfer cancelled successfully",
                    content = @Content(schema = @Schema(implementation = ScheduledTransferResponse.class))
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
                    responseCode = "403",
                    description = "Forbidden - not the owner"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<ScheduledTransferResponse> cancelScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id) {
        // BUG-BE-148: Verify ownership before cancelling
        verifyOwnership(id);

        ScheduledTransferResponse response = scheduledTransferUseCase.cancelScheduledTransfer(id);
        return ResponseEntity.ok(response);
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
                    responseCode = "200",
                    description = "Scheduled transfer paused successfully",
                    content = @Content(schema = @Schema(implementation = ScheduledTransferResponse.class))
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
                    responseCode = "403",
                    description = "Forbidden - not the owner"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<ScheduledTransferResponse> pauseScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id) {
        // BUG-BE-148: Verify ownership before pausing
        verifyOwnership(id);

        ScheduledTransferResponse response = scheduledTransferUseCase.pauseScheduledTransfer(id);
        return ResponseEntity.ok(response);
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
                    responseCode = "200",
                    description = "Scheduled transfer resumed successfully",
                    content = @Content(schema = @Schema(implementation = ScheduledTransferResponse.class))
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
                    responseCode = "403",
                    description = "Forbidden - not the owner"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Scheduled transfer not found"
            )
    })
    public ResponseEntity<ScheduledTransferResponse> resumeScheduledTransfer(
            @Parameter(description = "Scheduled transfer ID", required = true)
            @PathVariable UUID id) {
        // BUG-BE-148: Verify ownership before resuming
        verifyOwnership(id);

        ScheduledTransferResponse response = scheduledTransferUseCase.resumeScheduledTransfer(id);
        return ResponseEntity.ok(response);
    }
}
