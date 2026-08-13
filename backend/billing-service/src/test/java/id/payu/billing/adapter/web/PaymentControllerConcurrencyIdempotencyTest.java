package id.payu.billing.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.billing.application.service.PaymentService;
import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.model.PaymentStatus;
import id.payu.commons.idempotency.IdempotencyEntry;
import id.payu.commons.idempotency.IdempotencyInterceptor;
import id.payu.commons.idempotency.IdempotencyKey;
import id.payu.commons.idempotency.IdempotencyRepository;
import id.payu.commons.idempotency.IdempotencyService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * QAMVP-011 (billing): 10 concurrent requests with the same
 * X-Idempotency-Key against the bill payment money path must produce exactly
 * 1 mutation. Same harness as wallet/transaction variants.
 */
@DisplayName("QAMVP-011 — billing idempotency concurrency: 10 threads, 1 mutation")
class PaymentControllerConcurrencyIdempotencyTest {

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

    private static MockMvc buildMockMvc(PaymentService paymentService, IdempotencyInterceptor interceptor) {
        return MockMvcBuilders
                .standaloneSetup(new PaymentController(paymentService))
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
    @DisplayName("10 concurrent duplicate keys: exactly 1 bill payment mutation, 1 atomic claim")
    void tenConcurrentDuplicateKeysProduceExactlyOneMutation() throws Exception {
        ThreadSafeInMemoryRepository repository = new ThreadSafeInMemoryRepository();
        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(service, new ObjectMapper());

        PaymentService paymentService = mock(PaymentService.class);
        BillPayment billPayment = mock(BillPayment.class);
        when(billPayment.getId()).thenReturn(UUID.randomUUID());
        when(billPayment.getReferenceNumber()).thenReturn("REF-001");
        when(billPayment.getAccountId()).thenReturn(ACCOUNT_ID);
        when(billPayment.getBillerType()).thenReturn(BillerType.PLN);
        when(billPayment.getCustomerId()).thenReturn("CUST-1");
        when(billPayment.getAmount()).thenReturn(new BigDecimal("10000"));
        when(billPayment.getAdminFee()).thenReturn(BigDecimal.ZERO);
        when(billPayment.getTotalAmount()).thenReturn(new BigDecimal("10000"));
        when(billPayment.getStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(billPayment.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());
        when(billPayment.getCompletedAt()).thenReturn(java.time.LocalDateTime.now());

        AtomicInteger mutations = new AtomicInteger();
        doAnswer(invocation -> {
            mutations.incrementAndGet();
            return billPayment;
        }).when(paymentService).createPayment(any(), anyString());

        MockMvc mockMvc = buildMockMvc(paymentService, interceptor);

        String idempotencyKey = UUID.randomUUID().toString();
        String body = "{\"accountId\":\"" + ACCOUNT_ID + "\",\"billerCode\":\"PLN\",\"customerId\":\"CUST-1\","
                + "\"amount\":10000}";

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
                            int resultStatus = mockMvc.perform(post("/api/v1/payments")
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
                .as("10 identical concurrent bill payment requests must produce exactly 1 money mutation")
                .isEqualTo(1);
        assertThat(repository.successfulClaims.get())
                .as("exactly one atomic claim may win the idempotency race")
                .isEqualTo(1);
        assertThat(nonSuccess.get()).as("no request may 5xx").isZero();
        assertThat(exceptions.get()).as("no request may throw").isZero();
    }
}
