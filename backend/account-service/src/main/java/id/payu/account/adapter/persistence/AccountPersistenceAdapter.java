package id.payu.account.adapter.persistence;

import id.payu.account.domain.port.out.AccountPersistencePort;
import id.payu.account.adapter.persistence.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import id.payu.account.adapter.persistence.entity.AccountStatus;
import id.payu.account.adapter.persistence.entity.AccountType;

/**
 * Adapter implementation for AccountEntity persistence operations.
 * Converts between domain model and JPA entity.
 */
@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountPersistencePort {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public id.payu.account.domain.model.Account save(id.payu.account.domain.model.Account account) {
        id.payu.account.adapter.persistence.entity.AccountEntity entity = toEntity(account);
        id.payu.account.adapter.persistence.entity.AccountEntity savedEntity = accountRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<id.payu.account.domain.model.Account> findById(UUID id) {
        return accountRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<id.payu.account.domain.model.Account> findByExternalId(String externalId) {
        // External ID is not stored in accounts table - return empty
        // This would need to be added to schema if required
        return Optional.empty();
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    public Optional<id.payu.account.domain.model.Account> findByUserIdAndAllowPhoneLookupTrue(UUID userId) {
        return accountRepository.findByUserIdAndAllowPhoneLookupTrue(userId).map(this::toDomain);
    }

    @Override
    public java.util.List<id.payu.account.domain.model.Account> findByUserId(UUID userId) {
        return accountRepository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<id.payu.account.domain.model.Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).map(this::toDomain);
    }

    private id.payu.account.domain.model.Account toDomain(id.payu.account.adapter.persistence.entity.AccountEntity entity) {
        return id.payu.account.domain.model.Account.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .accountNumber(entity.getAccountNumber())
                .accountType(entity.getType() != null ? entity.getType().name() : null)
                .status(mapStatus(entity.getStatus()))
                .balance(entity.getBalance() != null ? entity.getBalance() : BigDecimal.ZERO)
                .currency(entity.getCurrency())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private id.payu.account.adapter.persistence.entity.AccountEntity toEntity(id.payu.account.domain.model.Account domain) {
        return id.payu.account.adapter.persistence.entity.AccountEntity.builder()
                .id(domain.getId())
                .accountNumber(domain.getAccountNumber())
                .type(mapAccountType(domain.getAccountType()))
                .status(mapAccountStatus(domain.getStatus()))
                .balance(domain.getBalance())
                .currency(domain.getCurrency())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private id.payu.account.domain.model.AccountStatus mapStatus(id.payu.account.adapter.persistence.entity.AccountStatus entityStatus) {
        if (entityStatus == null) {
            return id.payu.account.domain.model.AccountStatus.PENDING_VERIFICATION;
        }
        return switch (entityStatus) {
            case ACTIVE -> id.payu.account.domain.model.AccountStatus.ACTIVE;
            case CLOSED -> id.payu.account.domain.model.AccountStatus.CLOSED;
            case BLOCKED, DORMANT -> id.payu.account.domain.model.AccountStatus.FROZEN;
        };
    }

    private id.payu.account.adapter.persistence.entity.AccountStatus mapAccountStatus(id.payu.account.domain.model.AccountStatus domainStatus) {
        if (domainStatus == null) {
            return id.payu.account.adapter.persistence.entity.AccountStatus.DORMANT;
        }
        return switch (domainStatus) {
            case ACTIVE -> id.payu.account.adapter.persistence.entity.AccountStatus.ACTIVE;
            case CLOSED -> id.payu.account.adapter.persistence.entity.AccountStatus.CLOSED;
            case FROZEN, PENDING_VERIFICATION -> id.payu.account.adapter.persistence.entity.AccountStatus.BLOCKED;
        };
    }

    private id.payu.account.adapter.persistence.entity.AccountType mapAccountType(String accountType) {
        if (accountType == null) {
            return id.payu.account.adapter.persistence.entity.AccountType.POCKET;
        }
        try {
            return id.payu.account.adapter.persistence.entity.AccountType.valueOf(accountType);
        } catch (IllegalArgumentException e) {
            return id.payu.account.adapter.persistence.entity.AccountType.POCKET;
        }
    }
}
