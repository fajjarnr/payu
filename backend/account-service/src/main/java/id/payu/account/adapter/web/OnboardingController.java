package id.payu.account.adapter.web;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.in.RegisterUserUseCase;
import id.payu.account.dto.RegisterUserRequest;
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

    private final RegisterUserUseCase registerUserUseCase;

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<User>> register(@Valid @RequestBody RegisterUserRequest request) {
        return registerUserUseCase.registerUser(request)
                .thenApply(ResponseEntity::ok);
    }
}
