package id.payu.account.controller;

import id.payu.account.dto.RegisterUserRequest;
import id.payu.account.entity.User;
import id.payu.account.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<User>> register(@Valid @RequestBody RegisterUserRequest request) {
        return onboardingService.registerUser(request)
                .thenApply(ResponseEntity::ok);
    }
}
