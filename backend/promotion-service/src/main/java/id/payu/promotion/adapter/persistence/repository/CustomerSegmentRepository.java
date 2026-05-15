package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.CustomerSegmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegmentEntity, UUID> {

    List<CustomerSegmentEntity> findByIsActiveTrue();

    Optional<CustomerSegmentEntity> findByName(String name);

    boolean existsByName(String name);
}
