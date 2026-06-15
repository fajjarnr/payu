package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;

/**
 * Custom fragment for DisbursementJpaRepository.
 * See {@link DisbursementJpaRepositoryCustomImpl} for the implementation.
 */
public interface DisbursementJpaRepositoryCustom {
    void persistNew(DisbursementEntity entity);
}
