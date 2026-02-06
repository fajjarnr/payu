package id.payu.auth.controller;

import id.payu.auth.dto.*;
import id.payu.auth.exception.BiometricException;
import id.payu.auth.service.BiometricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biometric")
@RequiredArgsConstructor
@Tag(name = "Biometric Authentication", description = "Biometric authentication APIs for fingerprint and face recognition")
@SecurityRequirement(name = "bearerAuth")
public class BiometricController {

    private final BiometricService biometricService;

    @GetMapping("/challenge")
    @Operation(
            summary = "Generate biometric challenge",
            description = """
                    Generates a cryptographic challenge for biometric authentication.
                    The challenge is used to ensure freshness of the biometric signature.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Challenge generated successfully",
                    content = @Content(schema = @Schema(implementation = BiometricChallengeResponse.class))
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
    public Mono<ResponseEntity<BiometricChallengeResponse>> generateChallenge(
            @Parameter(description = "Username", required = true)
            @RequestParam String username,
            @Parameter(description = "Device identifier", required = true)
            @RequestParam String deviceId) {
        return Mono.fromCallable(() -> biometricService.generateChallenge(username, deviceId))
                .map(ResponseEntity::ok)
                .onErrorResume(BiometricException.class, e ->
                        Mono.just(ResponseEntity.badRequest().body(new BiometricChallengeResponse(
                                null, null, null, e.getMessage()))))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.internalServerError().body(new BiometricChallengeResponse(
                                null, null, null, "Internal server error"))));
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register biometric data",
            description = """
                    Registers a new biometric credential (fingerprint or face recognition)
                    for the authenticated user. The biometric data is securely stored
                    and linked to the user's account.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Biometric registered successfully",
                    content = @Content(schema = @Schema(implementation = BiometricRegistrationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid biometric data | Registration failed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Biometric already registered for this device"
            )
    })
    public Mono<ResponseEntity<BiometricRegistrationResponse>> register(
            @Parameter(description = "Biometric registration request", required = true)
            @Valid @RequestBody BiometricRegistrationRequest request) {
        return Mono.fromCallable(() -> biometricService.registerBiometric(request))
                .map(ResponseEntity::ok)
                .onErrorResume(BiometricException.class, e ->
                        Mono.just(ResponseEntity.badRequest().body(new BiometricRegistrationResponse(
                                null, null, null, null, null, null, e.getMessage()))))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.internalServerError().body(new BiometricRegistrationResponse(
                                null, null, null, null, null, null, "Internal server error"))));
    }

    @PostMapping("/authenticate")
    @Operation(
            summary = "Authenticate with biometric",
            description = """
                    Authenticates a user using their registered biometric credential.
                    The user must provide the biometric signature and the challenge response.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = BiometricAuthenticationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid biometric data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication failed | Invalid signature"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Biometric registration not found"
            )
    })
    public Mono<ResponseEntity<BiometricAuthenticationResponse>> authenticate(
            @Parameter(description = "Biometric authentication request", required = true)
            @Valid @RequestBody BiometricAuthenticationRequest request) {
        return Mono.fromCallable(() -> biometricService.findRegistration(request.username(), request.deviceId()))
                .flatMap(registration -> {
                    if (registration.isEmpty()) {
                        return Mono.error(new BiometricException("BIO_001", "Biometric registration not found"));
                    }
                    return Mono.fromCallable(() -> biometricService.authenticateWithBiometric(request, registration.get()));
                })
                .map(ResponseEntity::ok)
                .onErrorResume(BiometricException.class, e ->
                        Mono.just(ResponseEntity.status(401).body(new BiometricAuthenticationResponse(
                                null, null, null, null, null, null, e.getMessage()))))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.internalServerError().body(new BiometricAuthenticationResponse(
                                null, null, null, null, null, null, "Internal server error"))));
    }

    @GetMapping("/registrations/{username}")
    @Operation(
            summary = "Get user biometric registrations",
            description = """
                    Retrieves all biometric registrations for a specific user.
                    Useful for showing the user which devices have biometric access enabled.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of biometric registrations"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - can only view own registrations"
            )
    })
    public Mono<ResponseEntity<List<BiometricRegistration>>> getUserRegistrations(
            @Parameter(description = "Username", required = true)
            @PathVariable String username) {
        return Mono.fromCallable(() -> biometricService.getUserRegistrations(username))
                .map(ResponseEntity::ok)
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.internalServerError().body(List.of())));
    }

    @DeleteMapping("/registrations/{registrationId}")
    @Operation(
            summary = "Revoke biometric registration",
            description = """
                    Revokes a previously registered biometric credential.
                    The biometric data is securely deleted and can no longer be used for authentication.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Registration revoked successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - can only revoke own registrations"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Registration not found"
            )
    })
    public Mono<ResponseEntity<String>> revokeRegistration(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable String registrationId) {
        return Mono.fromRunnable(() -> biometricService.revokeRegistration(registrationId))
                .then(Mono.just(ResponseEntity.ok("Registration revoked successfully")))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.internalServerError().body("Failed to revoke registration")));
    }
}
