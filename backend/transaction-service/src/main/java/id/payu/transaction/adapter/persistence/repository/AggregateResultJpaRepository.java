package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.AggregateResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AggregateResultJpaRepository extends JpaRepository<AggregateResultEntity, UUID> {
    List<AggregateResultEntity> findByReferenceNoOrderByFanoutOrderDesc(String referenceNo);
    List<AggregateResultEntity> findByReferenceNo(String referenceNo);
}
