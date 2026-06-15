package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;

/**
 * Custom fragment for ScheduledTransferJpaRepository.
 * See {@link ScheduledTransferJpaRepositoryCustomImpl}.
 */
public interface ScheduledTransferJpaRepositoryCustom {
    void persistNew(ScheduledTransferEntity entity);
}
