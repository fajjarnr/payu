package id.payu.account.adapter.web;

import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.dto.PhoneLookupResponse;
import id.payu.account.entity.User;
import id.payu.account.repository.AccountRepository;
import id.payu.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Lookup", description = "P2P account lookup endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AccountLookupController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @GetMapping("/lookup")
    @Operation(summary = "Lookup account by phone number (IMP-036)")
    @PreAuthorize("hasAuthority('read:account')")
    public ResponseEntity<ApiResponse<PhoneLookupResponse>> lookupByPhone(
            @Parameter(description = "Phone number (e.g., 08123456789)", required = true)
            @RequestParam String phone) {
        log.info("Looking up account by phone: {}", phone != null && phone.length() > 6 ? phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3) : "***");

        // Find user by phone number
        User user = userRepository.findByPhoneNumber(phone).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success(PhoneLookupResponse.notFound()));
        }

        // Find active account with phone lookup enabled
        var account = accountRepository.findByUserIdAndAllowPhoneLookupTrue(user.getId()).orElse(null);
        if (account == null) {
            return ResponseEntity.ok(ApiResponse.success(PhoneLookupResponse.notFound()));
        }

        // Mask account number - show only last 4 digits
        String accountNumber = account.getAccountNumber();
        String maskedNumber = maskAccountNumber(accountNumber);

        // Get account name from user's profile or username
        String accountName = user.getUsername();

        return ResponseEntity.ok(ApiResponse.success(
                PhoneLookupResponse.found(accountName, maskedNumber)));
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        int length = accountNumber.length();
        return "*".repeat(length - 4) + accountNumber.substring(length - 4);
    }
}
