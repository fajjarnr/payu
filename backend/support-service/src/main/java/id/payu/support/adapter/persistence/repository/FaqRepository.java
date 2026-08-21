package id.payu.support.adapter.persistence.repository;

import id.payu.support.adapter.persistence.entity.FaqEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FaqRepository extends JpaRepository<FaqEntity, UUID> {
    List<FaqEntity> findByCategory(String category);
}
