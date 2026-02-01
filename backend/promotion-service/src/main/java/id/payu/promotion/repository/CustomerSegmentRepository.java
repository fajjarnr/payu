package id.payu.promotion.repository;

import id.payu.promotion.domain.CustomerSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, UUID> {

    List<CustomerSegment> findByActiveTrue();

    Optional<CustomerSegment> findByCode(String code);

    boolean existsByCode(String code);
}
