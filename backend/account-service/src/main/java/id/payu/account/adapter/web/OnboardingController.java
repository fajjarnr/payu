package id.payu.account.adapter.web;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.in.RegisterUserUseCase;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Onboarding", description = "User registration and account creation APIs")
@RequiredArgsConstructor
public class OnboardingController {

    private final RegisterUserUseCase registerUserUseCase;

    @GetMapping
    @Operation(summary = "Account service status", description = "Returns account service health and available endpoints")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<Map<String, Object>>> getAccountStatus() {
        return ResponseEntity.ok(id.payu.api.common.response.ApiResponse.success(Map.of(
                "service", "account-service",
                "status", "UP",
                "version", "1.0.0"
        )));
    }

    @PostMapping("/register")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.CREATE,
            entityType = "User",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(summary = "Register new user", description = "Create a new user account with email and password")
    @ApiResponse(responseCode = "200", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "User already exists")
    public CompletableFuture<ResponseEntity<User>> register(@Valid @RequestBody RegisterUserRequest request) {
        return registerUserUseCase.registerUser(request)
                .orTimeout(30, TimeUnit.SECONDS) // BUG-BE-140: Prevent indefinite hang on async registration
                .thenApply(ResponseEntity::ok);
    }
}
