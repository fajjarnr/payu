package id.payu.backoffice.adapter.persistence;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import id.payu.backoffice.adapter.persistence.repository.KycReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class KycPiiBackfillBatchTest {

    @Test
    void claimsRowsWithSkipLockedAndWritesCurrentKeyVersion() throws Exception {
        KycReviewRepository repository = mock(KycReviewRepository.class);
        BlindIndexService blindIndex = new BlindIndexService("01234567890123456789012345678901", "v2", "");
        KycReviewEntity entity = new KycReviewEntity();
        entity.setUserId("User-123");
        when(repository.lockNextPiiMigrationBatch("v2", 100)).thenReturn(List.of(entity));

        int migrated = new KycPiiBackfillBatch(repository, blindIndex).migrateNextBatch();

        assertEquals(1, migrated);
        assertEquals("v2", entity.getUserIdBlindIndexKeyVersion());
        assertEquals(blindIndex.index("User-123"), entity.getUserIdBlindIndex());
        verify(repository).saveAllAndFlush(List.of(entity));

        Query query = KycReviewRepository.class
                .getMethod("lockNextPiiMigrationBatch", String.class, int.class)
                .getAnnotation(Query.class);
        assertTrue(query.value().contains("FOR UPDATE SKIP LOCKED"));
    }

    @Test
    void scheduledRunnerRetriesOnNextInvocationAfterFailure() {
        KycPiiBackfillBatch batch = mock(KycPiiBackfillBatch.class);
        when(batch.migrateNextBatch()).thenThrow(new IllegalStateException("conflict")).thenReturn(1);
        KycPiiBackfillRunner runner = new KycPiiBackfillRunner(batch);

        runner.runNextBatch();
        runner.runNextBatch();

        verify(batch, times(2)).migrateNextBatch();
    }
}
