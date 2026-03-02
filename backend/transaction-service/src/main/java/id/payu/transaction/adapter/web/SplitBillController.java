package id.payu.transaction.adapter.web;

import id.payu.transaction.application.security.SplitBillSecurityService;
import id.payu.transaction.domain.model.SplitBill;
import id.payu.transaction.domain.port.in.SplitBillUseCase;
import id.payu.transaction.dto.AddParticipantRequest;
import id.payu.transaction.dto.CreateSplitBillRequest;
import id.payu.transaction.dto.MakePaymentRequest;
import id.payu.transaction.dto.SplitBillResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/split-bills")
@Tag(name = "Split Bill", description = "APIs for managing split bills between multiple users")
public class SplitBillController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SplitBillController.class);



    private final SplitBillUseCase splitBillUseCase;
    private final SplitBillSecurityService splitBillSecurityService;

    public SplitBillController(SplitBillUseCase splitBillUseCase, SplitBillSecurityService splitBillSecurityService) {
        this.splitBillUseCase = splitBillUseCase;
        this.splitBillSecurityService = splitBillSecurityService;
    }

    @PostMapping
    @Operation(summary = "Create a new split bill", description = "Create a new split bill and add initial participants")
    @PreAuthorize("isAuthenticated()")
    @ApiResponse(responseCode = "201", description = "Split bill created successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<SplitBillResponse> createSplitBill(@Valid @RequestBody CreateSplitBillRequest request) {
        log.info("Creating split bill: title={}, amount={}", request.getTitle(), request.getTotalAmount());
        SplitBillResponse response = splitBillUseCase.createSplitBill(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.hasReadAccess(#id, authentication.principal.userId)")
    @Operation(summary = "Get split bill by ID", description = "Retrieve details of a specific split bill")
    @ApiResponse(responseCode = "200", description = "Split bill found")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not a participant")
    public ResponseEntity<SplitBillResponse> getSplitBill(@PathVariable UUID id) {
        log.info("Getting split bill: id={}", id);
        SplitBillResponse response = splitBillUseCase.getSplitBill(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get account split bills", description = "Retrieve all split bills for an account")
    @PreAuthorize("isAuthenticated() and #accountId == authentication.principal.accountId")
    public ResponseEntity<List<SplitBill>> getAccountSplitBills(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting split bills for account: id={}", accountId);
        List<SplitBill> splitBills = splitBillUseCase.getAccountSplitBills(accountId, page, size);
        return ResponseEntity.ok(splitBills);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.isOwner(#id, authentication.principal.userId)")
    @Operation(summary = "Update split bill", description = "Update details of a draft split bill")
    @ApiResponse(responseCode = "200", description = "Split bill updated successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not the creator")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    public ResponseEntity<SplitBillResponse> updateSplitBill(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSplitBillRequest request) {
        log.info("Updating split bill: id={}", id);
        SplitBillResponse response = splitBillUseCase.updateSplitBill(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.isOwner(#id, authentication.principal.userId)")
    @Operation(summary = "Cancel split bill", description = "Cancel a split bill in draft or active status")
    @ApiResponse(responseCode = "200", description = "Split bill cancelled successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not the creator")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    public ResponseEntity<SplitBillResponse> cancelSplitBill(@PathVariable UUID id) {
        log.info("Cancelling split bill: id={}", id);
        SplitBillResponse response = splitBillUseCase.cancelSplitBill(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.isOwner(#id, authentication.principal.userId)")
    @Operation(summary = "Activate split bill", description = "Activate a draft split bill and send notifications to participants")
    @ApiResponse(responseCode = "200", description = "Split bill activated successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not the creator")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    public ResponseEntity<SplitBillResponse> activateSplitBill(@PathVariable UUID id) {
        log.info("Activating split bill: id={}", id);
        SplitBillResponse response = splitBillUseCase.activateSplitBill(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/participants")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.isOwner(#id, authentication.principal.userId)")
    @Operation(summary = "Add participant", description = "Add a new participant to a draft split bill")
    @ApiResponse(responseCode = "200", description = "Participant added successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not the creator")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    public ResponseEntity<SplitBillResponse> addParticipant(
            @PathVariable UUID id,
            @Valid @RequestBody AddParticipantRequest request) {
        log.info("Adding participant to split bill: id={}", id);
        SplitBillResponse response = splitBillUseCase.addParticipant(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/participants/{participantId}/accept")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.canRespondToInvitation(#id, #participantId, authentication.principal.userId)")
    @Operation(summary = "Accept split bill", description = "Accept a split bill invitation")
    @ApiResponse(responseCode = "200", description = "Split bill accepted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not the participant")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    public ResponseEntity<SplitBillResponse> acceptSplitBill(
            @PathVariable UUID id,
            @PathVariable UUID participantId) {
        log.info("Accepting split bill: id={}, participantId={}", id, participantId);
        SplitBillResponse response = splitBillUseCase.acceptSplitBill(id, participantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/participants/{participantId}/decline")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.canRespondToInvitation(#id, #participantId, authentication.principal.userId)")
    @Operation(summary = "Decline split bill", description = "Decline a split bill invitation")
    @ApiResponse(responseCode = "200", description = "Split bill declined successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not the participant")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    public ResponseEntity<SplitBillResponse> declineSplitBill(
            @PathVariable UUID id,
            @PathVariable UUID participantId) {
        log.info("Declining split bill: id={}, participantId={}", id, participantId);
        SplitBillResponse response = splitBillUseCase.declineSplitBill(id, participantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/participants/{participantId}/payment")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.canMakePayment(#id, #participantId, authentication.principal.userId)")
    @Operation(summary = "Make payment", description = "Make a payment towards a split bill")
    @ApiResponse(responseCode = "200", description = "Payment made successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not the participant")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    public ResponseEntity<SplitBillResponse> makePayment(
            @PathVariable UUID id,
            @PathVariable UUID participantId,
            @Valid @RequestBody MakePaymentRequest request) {
        log.info("Making payment for split bill: id={}, participantId={}, amount={}",
                id, participantId, request.getAmount());
        SplitBillResponse response = splitBillUseCase.makePayment(id, participantId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/settle")
    @PreAuthorize("isAuthenticated() and @splitBillSecurityService.isOwner(#id, authentication.principal.userId)")
    @Operation(summary = "Settle split bill", description = "Mark a split bill as completed")
    @ApiResponse(responseCode = "200", description = "Split bill settled successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - not the creator")
    @ApiResponse(responseCode = "404", description = "Split bill not found")
    public ResponseEntity<SplitBillResponse> settleSplitBill(@PathVariable UUID id) {
        log.info("Settling split bill: id={}", id);
        SplitBillResponse response = splitBillUseCase.settleSplitBill(id);
        return ResponseEntity.ok(response);
    }
}
