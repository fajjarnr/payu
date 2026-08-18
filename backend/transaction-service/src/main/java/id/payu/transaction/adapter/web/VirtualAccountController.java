package id.payu.transaction.adapter.web;

import id.payu.api.common.controller.BaseController;
import id.payu.api.common.openapi.OpenApiConstants;
import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;
import id.payu.transaction.application.service.VirtualAccountService;
import id.payu.transaction.interfaces.dto.CreateVirtualAccountRequest;
import id.payu.transaction.interfaces.dto.VaCallbackRequest;
import id.payu.transaction.interfaces.dto.VirtualAccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import id.payu.security.annotation.AuditOperation;

/**
 * REST controller for Virtual Account payment collection.
 * Partners create VA numbers for customers to pay to.
 */
@RestController
@RequestMapping("/api/v1/payments/va")
@Tag(name = "Virtual Accounts", description = "Virtual Account payment collection endpoints")
@SecurityRequirement(name = "bearerAuth")
public class VirtualAccountController extends BaseController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VirtualAccountController.class);



    private final VirtualAccountService virtualAccountService;

    public VirtualAccountController(VirtualAccountService virtualAccountService) {
        this.virtualAccountService = virtualAccountService;
    }

    @PostMapping
    @Operation(summary = "Create a Virtual Account",
               description = "Generate a VA number at the specified bank for payment collection. Restricted to PARTNER/ADMIN roles.")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    @Audited(operation = AuditOperation.CREATE, entityType = "VirtualAccountEntity", level = AuditLevel.INFO)
    @Idempotent(required = true)
    public ResponseEntity<ApiResponse<VirtualAccountResponse>> create(
            @Valid @RequestBody CreateVirtualAccountRequest request) {
        VirtualAccountResponse response = virtualAccountService.createVirtualAccount(request);
        return created(response, "/api/v1/payments/va/" + response.getId());
    }

    @GetMapping("/{vaId}")
    @Operation(summary = "Get VA details by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VirtualAccountResponse>> getById(@PathVariable UUID vaId) {
        return ok(virtualAccountService.getById(vaId));
    }

    @GetMapping("/number/{vaNumber}")
    @Operation(summary = "Get VA details by VA number")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VirtualAccountResponse>> getByNumber(@PathVariable String vaNumber) {
        return ok(virtualAccountService.getByVaNumber(vaNumber));
    }

    @PostMapping("/callback")
    @Idempotent(required = true)
    @Operation(summary = "Bank callback for VA payment confirmation",
               description = "Called by bank (simulated) when customer pays to the VA number")
    @Audited(operation = AuditOperation.UPDATE, entityType = "VirtualAccountEntity", level = AuditLevel.INFO)
    public ResponseEntity<ApiResponse<VirtualAccountResponse>> bankCallback(
            @Valid @RequestBody VaCallbackRequest callback) {
        log.info("Received VA callback for VA={}, amount={}", callback.getVaNumber(), callback.getAmount());
        VirtualAccountResponse response = virtualAccountService.handleBankCallback(callback);
        return ok(response);
    }
}
