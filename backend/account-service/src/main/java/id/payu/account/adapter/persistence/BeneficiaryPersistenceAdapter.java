package id.payu.account.adapter.persistence;

import id.payu.account.adapter.persistence.entity.BeneficiaryEntity;
import id.payu.account.adapter.persistence.entity.UserEntity;
import id.payu.account.adapter.persistence.repository.BeneficiaryRepository;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.domain.model.Beneficiary;
import id.payu.account.domain.model.BeneficiaryStatus;
import id.payu.account.domain.port.out.BeneficiaryPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BeneficiaryPersistenceAdapter implements BeneficiaryPersistencePort {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    @Override
    public Beneficiary save(Beneficiary beneficiary) {
        BeneficiaryEntity entity = toEntity(beneficiary);
        BeneficiaryEntity saved = beneficiaryRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Beneficiary> findById(UUID id) {
        return beneficiaryRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Beneficiary> findActiveByUserId(UUID userId) {
        return beneficiaryRepository.findActiveByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countActiveByUserId(UUID userId) {
        return beneficiaryRepository.countActiveByUserId(userId);
    }

    @Override
    public Optional<Beneficiary> findByUserIdAndBankCodeAndAccountNumber(UUID userId, String bankCode, String accountNumber) {
        return beneficiaryRepository.findByUserIdAndBankCodeAndAccountNumber(userId, bankCode, accountNumber)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByUserIdAndBankCodeAndAccountNumber(UUID userId, String bankCode, String accountNumber) {
        return beneficiaryRepository.existsByUserIdAndBankCodeAndAccountNumber(userId, bankCode, accountNumber);
    }

    private BeneficiaryEntity toEntity(Beneficiary domain) {
        if (domain == null) return null;
        UserEntity user = null;
        if (domain.getUserId() != null) {
            user = userRepository.findById(domain.getUserId()).orElse(null);
        }
        return BeneficiaryEntity.builder()
                .id(domain.getId())
                .user(user)
                .tenantId(domain.getTenantId())
                .bankCode(domain.getBankCode())
                .accountNumber(domain.getAccountNumber())
                .accountName(domain.getAccountName())
                .nickname(domain.getNickname())
                .status(domain.getStatus())
                .verifiedAt(domain.getVerifiedAt())
                .build();
    }

    private Beneficiary toDomain(BeneficiaryEntity entity) {
        if (entity == null) return null;
        return Beneficiary.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .tenantId(entity.getTenantId())
                .bankCode(entity.getBankCode())
                .accountNumber(entity.getAccountNumber())
                .accountName(entity.getAccountName())
                .nickname(entity.getNickname())
                .status(entity.getStatus())
                .verifiedAt(entity.getVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
