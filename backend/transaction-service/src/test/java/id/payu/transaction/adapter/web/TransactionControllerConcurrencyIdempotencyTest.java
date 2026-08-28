package id.payu.transaction.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.commons.idempotency.IdempotencyEntry;
import id.payu.commons.idempotency.IdempotencyInterceptor;
import id.payu.commons.idempotency.IdempotencyKey;
import id.payu.commons.idempotency.IdempotencyRepository;
import id.payu.commons.idempotency.IdempotencyService;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.domain.port.out.StepUpVerificationPort;
import id.payu.transaction.interfaces.dto.InitiateTransferResponse;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * QAMVP-011 (transaction): 10 concurrent requests with the same
 * X-Idempotency-Key against the transfer money path must produce exactly 1
 * mutation. Same harness as the wallet variant: real
 * {@link IdempotencyInterceptor} + {@link IdempotencyService} with a
 * thread-safe in-memory repository, production body-caching filter replicated
 * via reflection.
 */
@DisplayName("QAMVP-011 — transaction idempotency concurrency: 10 threads, 1 mutation")
class TransactionControllerConcurrencyIdempotencyTest {

    private static final String ACCOUNT_ID = "acct-001";

    static final class ThreadSafeInMemoryRepository implements IdempotencyRepository {
        final ConcurrentHashMap<String, IdempotencyEntry> store = new ConcurrentHashMap<>();
        final AtomicInteger successfulClaims = new AtomicInteger();

        @Override
        public Optional<IdempotencyEntry> findByKey(IdempotencyKey key) {
            return Optional.ofNullable(store.get(key.value()));
        }

        @Override
        public boolean save(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
            store.put(key.value(), entry);
            return true;
        }

        @Override
        public boolean saveIfAbsent(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
            boolean claimed = store.putIfAbsent(key.value(), entry) == null;
            if (claimed) {
                successfulClaims.incrementAndGet();
            }
            return claimed;
        }

        @Override
        public void update(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
            store.put(key.value(), entry);
        }

        @Override
        public void delete(IdempotencyKey key) {
            store.remove(key.value());
        }

        @Override
        public boolean exists(IdempotencyKey key) {
            return store.containsKey(key.value());
        }

        @Override
        public long getTtl(IdempotencyKey key) {
            return store.containsKey(key.value()) ? 86400 : -1;
        }
    }

    private static Filter bodyCachingFilter() {
        try {
            Class<?> clazz = Class.forName("id.payu.commons.idempotency.IdempotencyRequestBodyFilter");
            var ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (Filter) ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("IdempotencyRequestBodyFilter not on test classpath", e);
        }
    }

    private static MockMvc buildMockMvc(TransactionUseCase useCase, IdempotencyInterceptor interceptor) {
        return MockMvcBuilders
                .standaloneSetup(new TransactionController(useCase, mock(
                        id.payu.transaction.application.service.AccountTransactionSummaryService.class),
                        mock(StepUpVerificationPort.class)))
                .addFilters(bodyCachingFilter())
                .addInterceptors(interceptor)
                .build();
    }

    private static void withAuthenticatedUser(Runnable action) {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("account_id", ACCOUNT_ID)
                .claim("sub", ACCOUNT_ID)
                .build();
        var auth = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {
            action.run();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
     }

    @Test
    @DisplayName("same key with different payload is a conflict, not a replay")
    void sameKeyDifferentPayloadIsConflict() throws Exception {
        ThreadSafeInMemoryRepository repository = new ThreadSafeInMemoryRepository();
        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(service, new ObjectMapper());

        TransactionUseCase useCase = mock(TransactionUseCase.class);
        InitiateTransferCommandResult result = mock(InitiateTransferCommandResult.class);
        when(result.toResponse()).thenReturn(new InitiateTransferResponse());
        when(result.transactionId()).thenReturn(UUID.randomUUID());
        when(useCase.initiateTransfer(any(InitiateTransferCommand.class))).thenReturn(result);

        AtomicInteger mutations = new AtomicInteger();
        doAnswer(invocation -> {
            mutations.incrementAndGet();
            return result;
        }).when(useCase).initiateTransfer(any(InitiateTransferCommand.class));

        MockMvc mockMvc = buildMockMvc(useCase, interceptor);

        String idempotencyKey = UUID.randomUUID().toString();
        String bodyA = "{\"senderAccountId\":\"" + UUID.randomUUID() + "\",\"recipientAccountNumber\":\"1234567890\","
                + "\"amount\":10000,\"currency\":\"IDR\",\"type\":\"INTERNAL_TRANSFER\",\"description\":\"test\"}";
        String bodyB = "{\"senderAccountId\":\"" + UUID.randomUUID() + "\",\"recipientAccountNumber\":\"1234567890\","
                + "\"amount\":99999,\"currency\":\"IDR\",\"type\":\"INTERNAL_TRANSFER\",\"description\":\"test\"}";

        AtomicInteger firstStatus = new AtomicInteger();
        AtomicInteger secondStatus = new AtomicInteger();
        withAuthenticatedUser(() -> {
            try {
                firstStatus.set(mockMvc.perform(post("/api/v1/transactions/transfer")
                                .header("X-Idempotency-Key", idempotencyKey)
                                .contentType("application/json").content(bodyA))
                        .andReturn().getResponse().getStatus());
                secondStatus.set(mockMvc.perform(post("/api/v1/transactions/transfer")
                                .header("X-Idempotency-Key", idempotencyKey)
                                .contentType("application/json").content(bodyB))
                        .andReturn().getResponse().getStatus());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(firstStatus.get()).isEqualTo(201);
        assertThat(secondStatus.get())
                .as("same key with a different payload must be rejected as a conflict, never replayed")
                .isEqualTo(409);
        assertThat(mutations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("10 concurrent duplicate keys: exactly 1 transfer mutation, 1 atomic claim")
    void tenConcurrentDuplicateKeysProduceExactlyOneMutation() throws Exception {
        ThreadSafeInMemoryRepository repository = new ThreadSafeInMemoryRepository();
        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(service, new ObjectMapper());

        TransactionUseCase useCase = mock(TransactionUseCase.class);
        InitiateTransferCommandResult result = mock(InitiateTransferCommandResult.class);
        when(result.toResponse()).thenReturn(new InitiateTransferResponse());
        when(result.transactionId()).thenReturn(UUID.randomUUID());
        when(useCase.initiateTransfer(any(InitiateTransferCommand.class))).thenReturn(result);

        AtomicInteger mutations = new AtomicInteger();
        doAnswer(invocation -> {
            mutations.incrementAndGet();
            return result;
        }).when(useCase).initiateTransfer(any(InitiateTransferCommand.class));

        MockMvc mockMvc = buildMockMvc(useCase, interceptor);

        String idempotencyKey = UUID.randomUUID().toString();
        String body = "{\"senderAccountId\":\"" + UUID.randomUUID() + "\",\"recipientAccountNumber\":\"1234567890\","
                + "\"amount\":10000,\"currency\":\"IDR\",\"type\":\"INTERNAL_TRANSFER\",\"description\":\"test\"}";

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger nonSuccess = new AtomicInteger();
        AtomicInteger exceptions = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    withAuthenticatedUser(() -> {
                        try {
                            int resultStatus = mockMvc.perform(post("/api/v1/transactions/transfer")
                                            .header("X-Idempotency-Key", idempotencyKey)
                                            .contentType("application/json")
                                            .content(body))
                                    .andReturn()
                                    .getResponse()
                                    .getStatus();
                            if (resultStatus >= 500) {
                                nonSuccess.incrementAndGet();
                            }
                        } catch (Exception e) {
                            exceptions.incrementAndGet();
                        }
                    });
                } catch (Exception e) {
                    exceptions.incrementAndGet();
                }
            });
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).as("all threads must start").isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).as("requests must finish").isTrue();

        assertThat(mutations.get())
                .as("10 identical concurrent transfer requests must produce exactly 1 money mutation")
                .isEqualTo(1);
        assertThat(repository.successfulClaims.get())
                .as("exactly one atomic claim may win the idempotency race")
                .isEqualTo(1);
        assertThat(nonSuccess.get()).as("no request may 5xx").isZero();
        assertThat(exceptions.get()).as("no request may throw").isZero();
    }
}
