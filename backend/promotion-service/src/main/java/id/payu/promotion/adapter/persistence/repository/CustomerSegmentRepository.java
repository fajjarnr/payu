package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.domain.CustomerSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, UUID> {

    List<CustomerSegment> findByIsActiveTrue();

    Optional<CustomerSegment> findByName(String name);

    boolean existsByName(String name);
}
