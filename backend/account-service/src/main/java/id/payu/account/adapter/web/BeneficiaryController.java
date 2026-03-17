package id.payu.account.adapter.web;

import id.payu.account.dto.BeneficiaryRequest;
import id.payu.account.dto.BeneficiaryResponse;
import id.payu.account.entity.Beneficiary;
import id.payu.account.repository.BeneficiaryRepository;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/beneficiaries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Beneficiaries", description = "Beneficiary management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BeneficiaryController {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    private static final int MAX_BENEFICIARIES = 50;

    @GetMapping
    @Operation(summary = "Get all beneficiaries for account")
    @PreAuthorize("hasAuthority('read:account')")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> getBeneficiaries(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Getting beneficiaries for account: {}", accountId);

        // BUG-BE-177: Validate that the accountId belongs to the authenticated user
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String authenticatedId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        if (!accountId.toString().equals(authenticatedId)) {
            log.warn("User {} attempted to access beneficiaries for account {}", authenticatedId, accountId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("BEN_004", "Access denied — account does not belong to authenticated user"));
        }

        List<Beneficiary> beneficiaries = beneficiaryRepository.findActiveByUserId(accountId);
        List<BeneficiaryResponse> responses = beneficiaries.stream()
                .map(BeneficiaryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping
    @Operation(summary = "Add a new beneficiary")
    @PreAuthorize("hasAuthority('write:account')")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> createBeneficiary(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId,
            @Valid @RequestBody BeneficiaryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Creating beneficiary for account: {}", accountId);

        // BUG-BE-177: Validate that the accountId belongs to the authenticated user
        // BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback
        String authenticatedId = jwt.getClaimAsString("account_id") != null ? jwt.getClaimAsString("account_id") : jwt.getSubject();
        if (!accountId.toString().equals(authenticatedId)) {
            log.warn("User {} attempted to create beneficiary for account {}", authenticatedId, accountId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("BEN_004", "Access denied — account does not belong to authenticated user"));
        }

        // Check beneficiary limit
        long count = beneficiaryRepository.countActiveByUserId(accountId);
        if (count >= MAX_BENEFICIARIES) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error("BEN_001", "Maximum " + MAX_BENEFICIARIES + " beneficiaries allowed"));
        }

        // Check for duplicates
        if (beneficiaryRepository.existsByUserIdAndBankCodeAndAccountNumber(
                accountId, request.getBankCode(), request.getAccountNumber())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("BEN_002", "Beneficiary already exists"));
        }

        // Get user
        var user = userRepository.findById(accountId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ACC_001", "Account not found"));
        }

        // TODO: Validate account via BI-FAST inquiry (IMP-035 requirement)
        // For now, we'll set a placeholder account name
        String accountName = request.getNickname() != null ? request.getNickname() : "Account Holder";

        Beneficiary beneficiary = Beneficiary.builder()
                .user(user)
                .tenantId(user.getTenantId())
                .bankCode(request.getBankCode())
                .accountNumber(request.getAccountNumber())
                .accountName(accountName)
                .nickname(request.getNickname())
                .status(Beneficiary.BeneficiaryStatus.ACTIVE)
                .verifiedAt(LocalDateTime.now())
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(BeneficiaryResponse.from(saved)));
    }

    @PutMapping("/{beneficiaryId}")
    @Operation(summary = "Update beneficiary nickname")
    @PreAuthorize("hasAuthority('write:account')")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> updateBeneficiary(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId,
            @Parameter(description = "Beneficiary ID", required = true)
            @PathVariable UUID beneficiaryId,
            @Valid @RequestBody BeneficiaryRequest request) {
        log.info("Updating beneficiary: {} for account: {}", beneficiaryId, accountId);

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId).orElse(null);
        if (beneficiary == null || !beneficiary.getUser().getId().equals(accountId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("BEN_003", "Beneficiary not found"));
        }

        beneficiary.setNickname(request.getNickname());
        beneficiary.setUpdatedAt(LocalDateTime.now());

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        return ResponseEntity.ok(ApiResponse.success(BeneficiaryResponse.from(saved)));
    }

    @DeleteMapping("/{beneficiaryId}")
    @Operation(summary = "Delete a beneficiary")
    @PreAuthorize("hasAuthority('write:account')")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId,
            @Parameter(description = "Beneficiary ID", required = true)
            @PathVariable UUID beneficiaryId) {
        log.info("Deleting beneficiary: {} for account: {}", beneficiaryId, accountId);

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId).orElse(null);
        if (beneficiary == null || !beneficiary.getUser().getId().equals(accountId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("BEN_003", "Beneficiary not found"));
        }

        beneficiary.setStatus(Beneficiary.BeneficiaryStatus.DELETED);
        beneficiary.setUpdatedAt(LocalDateTime.now());
        beneficiaryRepository.save(beneficiary);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
