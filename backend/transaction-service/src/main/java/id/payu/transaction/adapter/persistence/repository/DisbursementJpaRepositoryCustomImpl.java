package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/**
 * Custom repository fragment for DisbursementEntity that bypasses Spring Data
 * JPA's isNew() detection by calling EntityManager.persist() directly.
 *
 * <p>Per context7/spring-projects/spring-data-jpa documentation, when entities
 * have manually-assigned IDs and a null @Version, the default detection
 * treats them as "detached" and calls merge() (which fails with
 * StaleObjectStateException for new rows). EntityManager.persist() is the
 * correct JPA pattern for explicitly new entities.</p>
 */
@Repository
public class DisbursementJpaRepositoryCustomImpl implements DisbursementJpaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void persistNew(DisbursementEntity entity) {
        entityManager.persist(entity);
        entityManager.flush(); // force INSERT to surface any constraint violations
    }
}
