package id.payu.backoffice.adapter.persistence;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import id.payu.backoffice.adapter.persistence.repository.KycReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class KycPiiBackfillBatch {
    private static final int BATCH_SIZE = 100;

    private final KycReviewRepository repository;
    private final BlindIndexService blindIndex;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    int migrateNextBatch() {
        List<KycReviewEntity> batch = repository
                .lockNextPiiMigrationBatch(blindIndex.currentVersion(), BATCH_SIZE);
        for (KycReviewEntity entity : batch) {
            entity.setUserIdBlindIndex(blindIndex.index(entity.getUserId()));
            entity.setUserIdBlindIndexKeyVersion(blindIndex.currentVersion());
        }
        repository.saveAllAndFlush(batch);
        return batch.size();
    }
}
