package id.payu.account.service;

import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.entity.Profile;
import id.payu.account.entity.User;
import id.payu.account.repository.ProfileRepository;
import id.payu.account.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository; // To be created
    private final GatewayClient gatewayClient;

    @Transactional
    public User registerUser(RegisterUserRequest request) {
        log.info("Starting registration for email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        // 1. Initial NIK Check via Gateway -> Dukcapil Simulator
        // Note: In real world, we might do this via async or just check format first.
        // For simulation, we verify existence immediately.
        VerifyNikRequest verifyRequest = new VerifyNikRequest(
            request.nik(), 
            request.fullName(), 
            "UNKNOWN", // Simulator doesn't strictly check this for basic verify
            "2000-01-01" // Default
        );
        
        DukcapilResponse kycResponse;
        try {
            kycResponse = gatewayClient.verifyNik(verifyRequest);
        } catch (Exception e) {
            log.error("Failed to verify NIK: {}", e.getMessage());
            // Proceed but mark as PENDING verify if service is down? 
            // Or fail hard? For now, fail hard to demonstrate integration.
            throw new IllegalStateException("eKYC Service Unavailable: " + e.getMessage());
        }

        if (!kycResponse.verified()) {
            throw new IllegalArgumentException("NIK Verification Failed: " + kycResponse.responseMessage());
        }

        // 2. Create User
        User user = User.builder()
                .externalId(request.externalId())
                .username(request.username())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .status(User.UserStatus.ACTIVE) // Auto-activate for Phase 2
                .kycStatus(User.KycStatus.APPROVED) // Simplified flow
                .build();
        
        user = userRepository.save(user);

        // 3. Create Profile
        Profile profile = Profile.builder()
                .user(user)
                .fullName(request.fullName())
                .nik(request.nik())
                .build();
        
        profileRepository.save(profile);

        log.info("User registered successfully: userId={}", user.getId());
        return user;
    }
}
