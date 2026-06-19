package id.payu.account.adapter.persistence;

import id.payu.account.adapter.persistence.repository.AccountRepository;
import id.payu.account.adapter.persistence.repository.ProfileRepository;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.adapter.persistence.entity.ProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import id.payu.account.adapter.persistence.entity.KycStatus;
import id.payu.account.adapter.persistence.entity.UserStatus;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AccountRepository accountRepository;

    @Override
    public User save(User user) {
        id.payu.account.adapter.persistence.entity.UserEntity userEntity = toEntity(user);
        id.payu.account.adapter.persistence.entity.UserEntity savedEntity = userRepository.save(userEntity);
        
        // Save ProfileEntity if needed
        if (user.getFullName() != null || user.getNik() != null) {
            ProfileEntity profile = profileRepository.findById(savedEntity.getId())
                    .orElse(ProfileEntity.builder().user(savedEntity).build());
            
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

    @Override
    public Optional<User> findByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId).map(this::toDomain);
    }

    @Override
    public java.util.List<UUID> findAccountIdsByUserId(UUID userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(id.payu.account.adapter.persistence.entity.AccountEntity::getId)
                .collect(java.util.stream.Collectors.toList());
    }


    private User toDomain(id.payu.account.adapter.persistence.entity.UserEntity entity) {
        Optional<ProfileEntity> profileOpt = profileRepository.findById(entity.getId());
        
        return User.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .fullName(profileOpt.map(ProfileEntity::getFullName).orElse(null))
                .nik(profileOpt.map(ProfileEntity::getNik).orElse(null))
                .status(id.payu.account.domain.model.UserStatus.valueOf(entity.getStatus().name()))
                .kycStatus(id.payu.account.domain.model.KycStatus.valueOf(entity.getKycStatus().name()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private id.payu.account.adapter.persistence.entity.UserEntity toEntity(User domain) {
        return id.payu.account.adapter.persistence.entity.UserEntity.builder()
                .id(domain.getId())
                .externalId(domain.getExternalId())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .status(id.payu.account.adapter.persistence.entity.UserStatus.valueOf(domain.getStatus().name()))
                .kycStatus(id.payu.account.adapter.persistence.entity.KycStatus.valueOf(domain.getKycStatus().name()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
