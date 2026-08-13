package id.payu.wallet.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.commons.idempotency.IdempotencyEntry;
import id.payu.commons.idempotency.IdempotencyKey;
import id.payu.commons.idempotency.IdempotencyRepository;
import id.payu.commons.idempotency.IdempotencyService;
import id.payu.commons.idempotency.IdempotencyInterceptor;
import jakarta.servlet.Filter;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import id.payu.wallet.domain.port.in.WalletUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.util.Map;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QAMVP-011: 10 concurrent requests with the same X-Idempotency-Key against a
 * money mutation must yield exactly 1 mutation, 1 idempotency claim, 1 ledger
 * write — never 2. Drives the real {@link IdempotencyInterceptor} +
 * {@link IdempotencyService} against a thread-safe in-memory repository and
 * counts actual use-case invocations (the mutation).
 */
@DisplayName("QAMVP-011 — wallet idempotency concurrency: 10 threads, 1 mutation")
class WalletControllerConcurrencyIdempotencyTest {

    private static final String ACCOUNT_ID = "acct-001";

    /**
     * The production pipeline wraps the request in a caching filter
     * ({@code IdempotencyRequestBodyFilter}, package-private in api-commons) so
     * the interceptor can fingerprint the body without consuming it. Test
     * replicates that filter via reflection so the controller still sees the body.
     */
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

    private static void withAuthenticatedWalletOwner(Runnable action) {
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

    private static MockMvc buildMockMvc(WalletUseCase useCase, IdempotencyInterceptor interceptor) {
        return MockMvcBuilders
                .standaloneSetup(new WalletController(useCase, "payu-backend"))
                .addFilters(bodyCachingFilter(), new ResponseCachingFilter())
                .addInterceptors(interceptor)
                .build();
    }

    /**
     * Production wraps responses in {@code ContentCachingResponseWrapper}
     * (via a filter) so the interceptor can cache the response body. Replicate
     * it here — without it the idempotency entry stays IN_PROGRESS forever.
     */
    private static final class ResponseCachingFilter extends org.springframework.web.filter.OncePerRequestFilter {
        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        jakarta.servlet.FilterChain filterChain)
                throws jakarta.servlet.ServletException, java.io.IOException {
            filterChain.doFilter(request,
                    new org.springframework.web.util.ContentCachingResponseWrapper(response));
        }
    }

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

    @Test
    @DisplayName("10 concurrent duplicate keys: exactly 1 mutation, 1 atomic claim, 0 double-save")
    void tenConcurrentDuplicateKeysProduceExactlyOneMutation() throws Exception {
        ThreadSafeInMemoryRepository repository = new ThreadSafeInMemoryRepository();
        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(service, new ObjectMapper());

        WalletUseCase useCase = mock(WalletUseCase.class);
        AtomicInteger mutations = new AtomicInteger();
        doAnswer(invocation -> {
            mutations.incrementAndGet();
            return null;
        }).when(useCase).credit(any(), any(), any(), any());

        MockMvc mockMvc = buildMockMvc(useCase, interceptor);

        String idempotencyKey = UUID.randomUUID().toString();
        String body = "{\"amount\":10000,\"referenceId\":\"ref-1\",\"description\":\"topup\"}";

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger nonSuccess = new AtomicInteger();
        AtomicInteger exceptions = new AtomicInteger();
        java.util.List<Integer> statuses = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    withAuthenticatedWalletOwner(() -> {
                        try {
                            int result = mockMvc.perform(post("/api/v1/wallets/{accountId}/credit", ACCOUNT_ID)
                                            .header("X-Idempotency-Key", idempotencyKey)
                                            .contentType("application/json")
                                            .content(body))
                                    .andReturn()
                                    .getResponse()
                                    .getStatus();
                            statuses.add(result);
                            if (result >= 500) {
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
                .as("10 identical concurrent requests must produce exactly 1 money mutation; statuses=%s", statuses)
                .isEqualTo(1);
        assertThat(repository.successfulClaims.get())
                .as("exactly one atomic claim may win the idempotency race")
                .isEqualTo(1);
        assertThat(nonSuccess.get()).as("no request may 5xx").isZero();
        assertThat(exceptions.get()).as("no request may throw").isZero();
    }
}
