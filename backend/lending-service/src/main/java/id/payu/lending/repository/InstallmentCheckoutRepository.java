package id.payu.lending.repository;

import id.payu.lending.entity.InstallmentCheckoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentCheckoutRepository extends JpaRepository<InstallmentCheckoutEntity, UUID> {

    List<InstallmentCheckoutEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<InstallmentCheckoutEntity> findByExternalOrderId(String externalOrderId);
}
