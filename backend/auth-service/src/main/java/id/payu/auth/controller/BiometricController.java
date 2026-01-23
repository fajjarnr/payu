package id.payu.auth.controller;

import id.payu.auth.dto.*;
import id.payu.auth.exception.BiometricException;
import id.payu.auth.service.BiometricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biometric")
@RequiredArgsConstructor
public class BiometricController {

    private final BiometricService biometricService;

    @GetMapping("/challenge")
    public Mono<ResponseEntity<BiometricChallengeResponse>> generateChallenge(
            @RequestParam String username,
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
    public Mono<ResponseEntity<BiometricRegistrationResponse>> register(
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
    public Mono<ResponseEntity<BiometricAuthenticationResponse>> authenticate(
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
    public Mono<ResponseEntity<List<BiometricRegistration>>> getUserRegistrations(
            @PathVariable String username) {
        return Mono.fromCallable(() -> biometricService.getUserRegistrations(username))
                .map(ResponseEntity::ok)
                .onErrorResume(e -> 
                        Mono.just(ResponseEntity.internalServerError().body(List.of())));
    }

    @DeleteMapping("/registrations/{registrationId}")
    public Mono<ResponseEntity<String>> revokeRegistration(
            @PathVariable String registrationId) {
        return Mono.fromRunnable(() -> biometricService.revokeRegistration(registrationId))
                .then(Mono.just(ResponseEntity.ok("Registration revoked successfully")))
                .onErrorResume(e -> 
                        Mono.just(ResponseEntity.internalServerError().body("Failed to revoke registration")));
    }
}
