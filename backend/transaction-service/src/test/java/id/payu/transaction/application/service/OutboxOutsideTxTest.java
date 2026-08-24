package id.payu.transaction.application.service;

import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"payu.grpc.server.port=0"})
@ActiveProfiles("test")
@DirtiesContext
class OutboxOutsideTxTest {

    @Autowired DeferredOutboxService deferredOutboxService;
    @Autowired OutboxService outboxService;
    @Autowired OutboxRepository outboxRepository;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void deferredOutboxPublishesOutsideBusinessTxAfterCommit() {
        long before = outboxRepository.count();
        TransactionTemplate businessTx = new TransactionTemplate(txManager);
        businessTx.executeWithoutResult(tx -> {
            // inside business TX, publish via deferred (should register afterCommit, not yet visible)
            deferredOutboxService.publishAfterCommit("TestAggregate", "agg-1", "TestEvent",
                    Map.of("k","v"), "payu.transaction.test.v1");
            // inside same TX, outbox should NOT yet be visible (outside TX)
            // count still same before commit
            // Note: REQUIRES_NEW afterCommit hasn't fired yet
        });
        // after business TX commits, afterCommit should have fired and created outbox in REQUIRES_NEW
        long after = outboxRepository.count();
        assertThat(after).isGreaterThan(before);
        // verify the event was created
        assertThat(outboxRepository.findByAggregateId("agg-1")).isNotEmpty();
    }

    @Test
    void shedLockConfigUsesDbTimeAndUtcZone() {
        // verify configuration class has usingDbTime + withTimeZone(UTC)
        try {
            var src = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get("src/main/java/id/payu/transaction/config/ShedLockConfig.java")));
            assertThat(src).contains("usingDbTime()");
            assertThat(src).contains("withTimeZone");
            assertThat(src).contains("UTC");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
