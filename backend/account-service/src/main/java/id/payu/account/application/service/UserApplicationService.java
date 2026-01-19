package id.payu.account.application.service;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.in.RegisterUserUseCase;
import id.payu.account.domain.port.out.KycVerificationPort;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.RegisterUserRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserApplicationService implements RegisterUserUseCase {

    private final UserPersistencePort userPersistencePort;
    private final KycVerificationPort kycVerificationPort;

    @Override
    @Transactional
    @Async
    @CircuitBreaker(name = "dukcapilService", fallbackMethod = "registerFallback")
    @Retry(name = "dukcapilService")
    @TimeLimiter(name = "dukcapilService")
    public CompletableFuture<User> registerUser(RegisterUserRequest command) {
        log.info("Processing registration for user: {}", command.username());

        if (userPersistencePort.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userPersistencePort.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Call KYC (Port)
        DukcapilResponse kycResponse = kycVerificationPort.verifyNik(command.nik(), command.fullName());
        
        User.KycStatus kycStatus = kycResponse.verified() ? 
                User.KycStatus.APPROVED : User.KycStatus.REJECTED;

        User user = User.builder()
                .externalId(command.externalId())
                .username(command.username())
                .email(command.email())
                .phoneNumber(command.phoneNumber())
                .fullName(command.fullName())
                .nik(command.nik())
                .status(User.UserStatus.ACTIVE)
                .kycStatus(kycStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userPersistencePort.save(user);
        
        log.info("User registered successfully: {}", savedUser.getId());
        return CompletableFuture.completedFuture(savedUser);
    }

    public CompletableFuture<User> registerFallback(RegisterUserRequest command, Throwable t) {
        log.error("KYC Service unavailable, registering with PENDING status. Error: {}", t.getMessage());

        User user = User.builder()
                .externalId(command.externalId())
                .username(command.username())
                .email(command.email())
                .phoneNumber(command.phoneNumber())
                .fullName(command.fullName())
                .nik(command.nik())
                .status(User.UserStatus.PENDING_VERIFICATION)
                .kycStatus(User.KycStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userPersistencePort.save(user);
        return CompletableFuture.completedFuture(savedUser);
    }
}
