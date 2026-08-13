package id.payu.partner.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.commons.idempotency.IdempotencyEntry;
import id.payu.commons.idempotency.IdempotencyInterceptor;
import id.payu.commons.idempotency.IdempotencyKey;
import id.payu.commons.idempotency.IdempotencyRepository;
import id.payu.commons.idempotency.IdempotencyService;
import id.payu.partner.application.service.MerchantService;
import id.payu.partner.dto.QrPaymentResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * QAMVP-011 (partner): 10 concurrent requests with the same
 * X-Idempotency-Key against the QR payment money path must produce exactly
 * 1 mutation. Same harness as wallet/transaction/billing variants.
 */
@DisplayName("QAMVP-011 — partner idempotency concurrency: 10 threads, 1 mutation")
class MerchantControllerConcurrencyIdempotencyTest {

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

    private static MockMvc buildMockMvc(MerchantService merchantService, IdempotencyInterceptor interceptor) {
        return MockMvcBuilders
                .standaloneSetup(new MerchantController(merchantService))
                .addFilters(bodyCachingFilter())
                .addInterceptors(interceptor)
                .build();
     }

    @Test
    @DisplayName("same key with different payload is a conflict, not a replay")
    void sameKeyDifferentPayloadIsConflict() throws Exception {
        ThreadSafeInMemoryRepository repository = new ThreadSafeInMemoryRepository();
        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(service, new ObjectMapper());

        MerchantService merchantService = mock(MerchantService.class);
        QrPaymentResponse response = mock(QrPaymentResponse.class);
        when(merchantService.generateDynamicQr(eq(42L), any())).thenReturn(response);

        AtomicInteger mutations = new AtomicInteger();
        doAnswer(invocation -> {
            mutations.incrementAndGet();
            return response;
        }).when(merchantService).generateDynamicQr(eq(42L), any());

        MockMvc mockMvc = buildMockMvc(merchantService, interceptor);

        String idempotencyKey = UUID.randomUUID().toString();
        AtomicInteger firstStatus = new AtomicInteger();
        AtomicInteger secondStatus = new AtomicInteger();
        try {
            firstStatus.set(mockMvc.perform(post("/merchants/{merchantId}/qr", 42L)
                            .header("X-Idempotency-Key", idempotencyKey)
                            .contentType("application/json")
                            .content("{\"amount\":10000,\"currency\":\"IDR\"}"))
                    .andReturn().getResponse().getStatus());
            secondStatus.set(mockMvc.perform(post("/merchants/{merchantId}/qr", 42L)
                            .header("X-Idempotency-Key", idempotencyKey)
                            .contentType("application/json")
                            .content("{\"amount\":99999,\"currency\":\"IDR\"}"))
                    .andReturn().getResponse().getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(firstStatus.get()).isEqualTo(201);
        assertThat(secondStatus.get())
                .as("same key with a different payload must be rejected as a conflict, never replayed")
                .isEqualTo(409);
        assertThat(mutations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("10 concurrent duplicate keys: exactly 1 QR payment mutation, 1 atomic claim")
    void tenConcurrentDuplicateKeysProduceExactlyOneMutation() throws Exception {
        ThreadSafeInMemoryRepository repository = new ThreadSafeInMemoryRepository();
        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(service, new ObjectMapper());

        MerchantService merchantService = mock(MerchantService.class);
        QrPaymentResponse response = mock(QrPaymentResponse.class);
        when(merchantService.generateDynamicQr(eq(42L), any())).thenReturn(response);

        AtomicInteger mutations = new AtomicInteger();
        doAnswer(invocation -> {
            mutations.incrementAndGet();
            return response;
        }).when(merchantService).generateDynamicQr(eq(42L), any());

        MockMvc mockMvc = buildMockMvc(merchantService, interceptor);

        String idempotencyKey = UUID.randomUUID().toString();
        String body = "{\"amount\":10000,\"currency\":\"IDR\",\"description\":\"qr test\"}";

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
                    int resultStatus = mockMvc.perform(post("/merchants/{merchantId}/qr", 42L)
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
                    throw new RuntimeException(e);
                }
            });
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).as("all threads must start").isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).as("requests must finish").isTrue();

        assertThat(mutations.get())
                .as("10 identical concurrent QR payment requests must produce exactly 1 mutation")
                .isEqualTo(1);
        assertThat(repository.successfulClaims.get())
                .as("exactly one atomic claim may win the idempotency race")
                .isEqualTo(1);
        assertThat(nonSuccess.get()).as("no request may 5xx").isZero();
        assertThat(exceptions.get()).as("no request may throw").isZero();
    }
}
