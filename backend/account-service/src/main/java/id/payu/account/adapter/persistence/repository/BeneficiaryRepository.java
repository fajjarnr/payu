package id.payu.account.adapter.persistence.repository;

import id.payu.account.adapter.persistence.entity.BeneficiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.account.adapter.persistence.entity.BeneficiaryStatus;

@Repository
public interface BeneficiaryRepository extends JpaRepository<BeneficiaryEntity, UUID> {

    List<BeneficiaryEntity> findByUserIdAndStatusNot(UUID userId, BeneficiaryStatus status);

    @Query("SELECT b FROM BeneficiaryEntity b WHERE b.user.id = :userId AND b.status != 'DELETED'")
    List<BeneficiaryEntity> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(b) FROM BeneficiaryEntity b WHERE b.user.id = :userId AND b.status != 'DELETED'")
    long countActiveByUserId(@Param("userId") UUID userId);

    Optional<BeneficiaryEntity> findByUserIdAndBankCodeAndAccountNumber(
            UUID userId, String bankCode, String accountNumber);

    boolean existsByUserIdAndBankCodeAndAccountNumber(
            UUID userId, String bankCode, String accountNumber);
}
