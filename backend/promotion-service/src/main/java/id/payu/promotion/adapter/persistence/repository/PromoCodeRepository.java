package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.PromoCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, UUID> {
    Optional<PromoCodeEntity> findByCode(String code);
}
