package id.payu.account.adapter.persistence;

import id.payu.account.adapter.persistence.repository.ProfileRepository;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.entity.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Override
    public User save(User user) {
        id.payu.account.entity.User userEntity = toEntity(user);
        id.payu.account.entity.User savedEntity = userRepository.save(userEntity);
        
        // Save Profile if needed
        if (user.getFullName() != null || user.getNik() != null) {
            Profile profile = profileRepository.findById(savedEntity.getId())
                    .orElse(Profile.builder().user(savedEntity).build());
            
            profile.setFullName(user.getFullName());
            profile.setNik(user.getNik());
            profileRepository.save(profile);
        }
        
        return toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    private User toDomain(id.payu.account.entity.User entity) {
        Optional<Profile> profileOpt = profileRepository.findById(entity.getId());
        
        return User.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .fullName(profileOpt.map(Profile::getFullName).orElse(null))
                .nik(profileOpt.map(Profile::getNik).orElse(null))
                .status(User.UserStatus.valueOf(entity.getStatus().name()))
                .kycStatus(User.KycStatus.valueOf(entity.getKycStatus().name()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private id.payu.account.entity.User toEntity(User domain) {
        return id.payu.account.entity.User.builder()
                .id(domain.getId())
                .externalId(domain.getExternalId())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .status(id.payu.account.entity.User.UserStatus.valueOf(domain.getStatus().name()))
                .kycStatus(id.payu.account.entity.User.KycStatus.valueOf(domain.getKycStatus().name()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
