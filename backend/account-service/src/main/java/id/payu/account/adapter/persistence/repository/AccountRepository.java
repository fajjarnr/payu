package id.payu.account.adapter.persistence.repository;

import id.payu.account.adapter.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findByUserId(UUID userId);
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    Optional<AccountEntity> findByUserIdAndAllowPhoneLookupTrue(UUID userId);
}
