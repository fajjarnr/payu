package id.payu.account.service;

import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.entity.Profile;
import id.payu.account.entity.User;
import id.payu.account.repository.ProfileRepository;
import id.payu.account.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final GatewayClient gatewayClient;

    @Transactional
    @CircuitBreaker(name = "gatewayService", fallbackMethod = "kycFallback")
    @Retry(name = "gatewayService", fallbackMethod = "kycFallback")
    @TimeLimiter(name = "gatewayService", fallbackMethod = "kycFallback")
    public CompletableFuture<User> registerUser(RegisterUserRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Starting registration for email: {}", request.email());

            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already registered");
            }
            if (userRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Username already taken");
            }

            VerifyNikRequest verifyRequest = new VerifyNikRequest(
                request.nik(),
                request.fullName(),
                "UNKNOWN",
                "2000-01-01"
            );

            DukcapilResponse kycResponse = gatewayClient.verifyNik(verifyRequest);

            if (!kycResponse.verified()) {
                throw new IllegalArgumentException("NIK Verification Failed: " + kycResponse.responseMessage());
            }

            User user = User.builder()
                    .externalId(request.externalId())
                    .username(request.username())
                    .email(request.email())
                    .phoneNumber(request.phoneNumber())
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.APPROVED)
                    .build();

            user = userRepository.save(user);

            Profile profile = Profile.builder()
                    .user(user)
                    .fullName(request.fullName())
                    .nik(request.nik())
                    .build();

            profileRepository.save(profile);

            log.info("User registered successfully: userId={}", user.getId());
            return user;
        });
    }

    private CompletableFuture<User> kycFallback(RegisterUserRequest request, Exception ex) {
        log.error("KYC verification failed or circuit opened: {}", ex.getMessage());
        throw new IllegalStateException("eKYC Service Unavailable. Please try again later.");
    }
}
