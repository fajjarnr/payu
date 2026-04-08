package id.payu.account.application.service;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.in.RegisterUserUseCase;
import id.payu.account.domain.port.out.IdentityProviderPort;
import id.payu.account.domain.port.out.KycVerificationPort;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.security.annotation.Audited;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserApplicationService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private final UserPersistencePort userPersistencePort;
    private final KycVerificationPort kycVerificationPort;
    private final IdentityProviderPort identityProviderPort;
    private final id.payu.account.domain.port.out.UserEventPublisherPort userEventPublisherPort;

    @Override
    // @Transactional ensures all JPA calls (existsBy*, save) share the same Hibernate session
    // and JDBC connection with proper transaction management. Without it, the existsBy* calls
    // run in autoCommit mode, polluting the connection state for the subsequent save.
    // KYC call is wrapped in try-catch and fails fast on error, so holding a tx is acceptable.
    // Resilience4j annotations removed — they caused double-fallback (duplicate key errors).
    @Transactional
    @Audited(operation = Audited.Operation.CREATE, entityType = "User")
    public CompletableFuture<User> registerUser(RegisterUserRequest command) {
        log.info("Processing registration for user: {}", command.username());

        if (userPersistencePort.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userPersistencePort.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Step 1: Provision identity in Keycloak BEFORE creating account.
        // If IAM provisioning fails, we don't create an orphaned DB record.
        String iamUserId = identityProviderPort.provisionUser(
            command.username(),
            command.email(),
            command.password(),
            command.fullName()
        );
        String externalId = iamUserId;
        if (externalId == null || externalId.isBlank()) {
            log.warn("Falling back to request externalId for user {} because IAM user id was not returned",
                command.username());
            externalId = command.externalId();
        }

        // Step 2: Attempt KYC verification — if unavailable, register with PENDING status.
        // Simple try-catch replaces resilience4j annotations which caused double-fallback
        // (both @CircuitBreaker and @Bulkhead fired registerFallback, leading to duplicate key errors).
        // Registration is low-throughput and non-idempotent — circuit breaking adds no value here.
        User.KycStatus kycStatus;
        User.UserStatus userStatus;
        try {
            DukcapilResponse kycResponse = kycVerificationPort.verifyNik(command.nik(), command.fullName());
            kycStatus = kycResponse.verified() ? User.KycStatus.APPROVED : User.KycStatus.REJECTED;
            // BUG-BE-027 Fix: Set user status based on KYC result.
            userStatus = kycResponse.verified()
                    ? User.UserStatus.ACTIVE
                    : User.UserStatus.PENDING_VERIFICATION;
        } catch (Exception e) {
            log.warn("KYC service unavailable, registering with PENDING status. Error: {}", e.getMessage());
            kycStatus = User.KycStatus.PENDING;
            userStatus = User.UserStatus.PENDING_VERIFICATION;
        }

        User user = User.builder()
            .externalId(externalId)
                .username(command.username())
                .email(command.email())
                .phoneNumber(command.phoneNumber())
                .fullName(command.fullName())
                .nik(command.nik())
                .status(userStatus)
                .kycStatus(kycStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser;
        try {
            savedUser = userPersistencePort.save(user);
        } catch (DataIntegrityViolationException e) {
            // BUG-BE-031: Handle race condition where concurrent registrations
            // both pass existsBy checks but one fails at DB unique constraint
            throw new IllegalStateException("Registration conflict: email or username already taken", e);
        }

        log.info("User registered successfully with status={}, kycStatus={}: {}",
                userStatus, kycStatus, savedUser.getId());

        // Publish event
        userEventPublisherPort.publishUserCreated(new id.payu.account.dto.UserCreatedEvent(
                savedUser.getId(),
                savedUser.getExternalId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getCreatedAt()));

        return CompletableFuture.completedFuture(savedUser);
    }
}
