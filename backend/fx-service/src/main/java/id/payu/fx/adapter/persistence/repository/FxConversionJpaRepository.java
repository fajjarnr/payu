package id.payu.fx.adapter.persistence.repository;

import id.payu.fx.adapter.persistence.entity.FxConversionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FxConversionJpaRepository extends JpaRepository<FxConversionEntity, UUID> {

    List<FxConversionEntity> findByAccountId(String accountId);

    void deleteById(UUID id);
}
