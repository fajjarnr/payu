package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/**
 * Custom fragment for ScheduledTransferJpaRepository.
 * Mirrors {@link DisbursementJpaRepositoryCustomImpl} for the
 * READY-063 fix pattern (manual id + @GeneratedValue causes
 * Spring Data JPA to call merge() which fails for new rows).
 */
public class ScheduledTransferJpaRepositoryCustomImpl implements ScheduledTransferJpaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void persistNew(ScheduledTransferEntity entity) {
        entityManager.persist(entity);
        entityManager.flush();
    }
}
