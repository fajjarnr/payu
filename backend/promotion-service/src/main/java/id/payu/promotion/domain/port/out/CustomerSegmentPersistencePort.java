package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.CustomerSegment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * BE-PROMO-001: persistence port for customer segments.
 */
public interface CustomerSegmentPersistencePort {

    CustomerSegment save(CustomerSegment segment);

    Optional<CustomerSegment> findById(UUID id);

    List<CustomerSegment> findAll();

    List<CustomerSegment> findByIsActiveTrue();

    boolean existsByName(String name);

    void deleteById(UUID id);
}
