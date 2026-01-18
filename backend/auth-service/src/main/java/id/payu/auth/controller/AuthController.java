package id.payu.auth.controller;

import id.payu.auth.dto.LoginRequest;
import id.payu.auth.dto.LoginResponse;
import id.payu.auth.service.KeycloakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = keycloakService.login(request.username(), request.password());
        return ResponseEntity.ok(response);
    }
    
    // Note: Registration is handled by Account Service, which creates user in database
    // and should trigger IAM creation. 
    // For now, Account Service creates "business" user, and we might sync later, 
    // or Account Service calls Auth Service to create IAM User.
    // Let's assume Account Service handles business logic, and maybe calls this for IAM sync if needed.
}
