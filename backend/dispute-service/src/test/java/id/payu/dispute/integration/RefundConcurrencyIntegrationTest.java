package id.payu.dispute.integration;

import id.payu.dispute.DisputeServiceApplication;
import id.payu.dispute.domain.model.TransactionDetails;
import id.payu.dispute.domain.port.in.RefundUseCase;
import id.payu.dispute.domain.port.out.RefundPersistencePort;
import id.payu.dispute.domain.port.out.TransactionLookupPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DISPUTE-001: concurrent partial refunds must never exceed the transaction
 * amount — the sum-then-check in assertRefundable must be serialized per
 * transaction (advisory lock), not just read-then-write.
 */
@SpringBootTest(classes = DisputeServiceApplication.class)
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@Import(RefundConcurrencyIntegrationTest.TestLookupConfiguration.class)
class RefundConcurrencyIntegrationTest {

    @TestConfiguration
    static class TestLookupConfiguration {
        @Bean
        @Primary
        TransactionLookupPort transactionLookupPort() {
            return transactionId -> java.util.Optional.of(
                    new TransactionDetails(new BigDecimal("100000.00"), "IDR"));
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dispute_service")
            .withUsername("payu")
            .withPassword("payu");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl().split("\\?")[0]);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RefundUseCase refundUseCase;

    @Autowired
    private RefundPersistencePort refundPersistencePort;

    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        refundPersistencePort.findAll().forEach(r -> refundPersistencePort.deleteById(r.getId()));
    }

    @Test
    @DisplayName("Two concurrent 60000 partial refunds on a 100000 transaction: exactly one wins")
    void concurrentPartialRefundsCannotExceedTransactionAmount() throws Exception {
        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    refundUseCase.createPartialRefund(
                            TRANSACTION_ID, new BigDecimal("60000.00"), "IDR",
                            "concurrent " + UUID.randomUUID());
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();

        long successes = 0;
        for (Future<Boolean> f : results) {
            successes += f.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
    }
}
