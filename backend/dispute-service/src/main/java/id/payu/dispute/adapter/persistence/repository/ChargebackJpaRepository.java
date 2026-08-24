package id.payu.dispute.adapter.persistence.repository;

import id.payu.dispute.adapter.persistence.entity.ChargebackEntity;
import id.payu.dispute.domain.model.ChargebackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChargebackJpaRepository extends JpaRepository<ChargebackEntity, UUID> {
    List<ChargebackEntity> findByStatus(ChargebackStatus status);
    List<ChargebackEntity> findByCustomerId(UUID customerId);
}
