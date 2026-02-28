package id.payu.account.repository;

import id.payu.account.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByUserIdAndStatusNot(UUID userId, Beneficiary.BeneficiaryStatus status);

    @Query("SELECT b FROM Beneficiary b WHERE b.user.id = :userId AND b.status != 'DELETED'")
    List<Beneficiary> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(b) FROM Beneficiary b WHERE b.user.id = :userId AND b.status != 'DELETED'")
    long countActiveByUserId(@Param("userId") UUID userId);

    Optional<Beneficiary> findByUserIdAndBankCodeAndAccountNumber(
            UUID userId, String bankCode, String accountNumber);

    boolean existsByUserIdAndBankCodeAndAccountNumber(
            UUID userId, String bankCode, String accountNumber);
}
