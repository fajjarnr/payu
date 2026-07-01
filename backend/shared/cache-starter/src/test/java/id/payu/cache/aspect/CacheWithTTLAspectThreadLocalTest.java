package id.payu.cache.aspect;

import id.payu.cache.annotation.CacheWithTTL;
import id.payu.cache.service.CacheService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * GAP-27 — Reproduces thread-local leakage in {@link CacheWithTTLAspect#handleSyncCache}.
 *
 * <p>Bug: the sync-cache stampede-protection path wraps {@code joinPoint.proceed()} in
 * {@code CompletableFuture.supplyAsync(...)} on the common ForkJoinPool. This detaches
 * the invocation from the original request thread, so {@code SecurityContextHolder},
 * {@code TenantContext}, MDC, and any active Hibernate transaction lose their
 * ThreadLocal bindings. Result: cross-tenant queries, missing audit principal,
 * broken {@code @Transactional} boundaries.</p>
 *
 * <p>This test asserts that the cached computation runs on the caller's thread with
 * its ThreadLocals intact. Before the fix it fails (thread is a ForkJoinPool worker,
 * ThreadLocals are null). After the fix it passes.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheWithTTLAspectThreadLocalTest {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> PRINCIPAL = new ThreadLocal<>();

    @Mock
    private CacheService cacheService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private CacheWithTTL cacheWithTTL;

    private CacheWithTTLAspect aspect;

    private final Executor refreshExecutor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void setUp() {
        aspect = new CacheWithTTLAspect(cacheService, refreshExecutor);
    }

    @AfterEach
    void tearDown() {
        TENANT.remove();
        PRINCIPAL.remove();
    }

    @Test
    void syncCacheMissMustRunProceedOnCallerThreadWithIntactThreadLocals() throws Throwable {
        Method sampleMethod = SampleService.class.getDeclaredMethod("getAccount", String.class);

        // Wire the @Around joinpoint + signature
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(signature.getReturnType()).thenReturn((Class) String.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"acc-1"});
        when(joinPoint.getTarget()).thenReturn(new SampleService());

        // Annotation: cache miss → null on first read; populate after proceed.
        when(cacheWithTTL.cacheName()).thenReturn("accounts");
        when(cacheWithTTL.key()).thenReturn("");
        when(cacheWithTTL.ttl()).thenReturn(60L);
        when(cacheWithTTL.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(cacheWithTTL.softTtlMultiplier()).thenReturn(0.5);
        when(cacheWithTTL.staleWhileRevalidate()).thenReturn(false);
        when(cacheWithTTL.condition()).thenReturn("");
        when(cacheWithTTL.unless()).thenReturn("");
        when(cacheWithTTL.sync()).thenReturn(true);

        // Cache miss — key is generated via Arrays.deepHashCode(args), so match any key
        lenient().when(cacheService.get(any(), eq(String.class))).thenReturn(null);
        // cacheService.put returns void — no stub needed; lenient() allows unused stub if any
        lenient().doNothing().when(cacheService).put(any(), any(), any());

        // Capture execution-thread identity + ThreadLocals as seen from inside proceed()
        AtomicReference<Thread> proceedThread = new AtomicReference<>();
        AtomicReference<String> proceedTenant = new AtomicReference<>();
        AtomicReference<String> proceedPrincipal = new AtomicReference<>();
        CountDownLatch proceedDone = new CountDownLatch(1);

        when(joinPoint.proceed()).thenAnswer(invocation -> {
            proceedThread.set(Thread.currentThread());
            proceedTenant.set(TENANT.get());
            proceedPrincipal.set(PRINCIPAL.get());
            proceedDone.countDown();
            return "result-for-acc-1";
        });

        // Set ThreadLocals on the caller thread
        TENANT.set("tenant-bravo");
        PRINCIPAL.set("user-42");
        Thread callerThread = Thread.currentThread();

        // Act — must block until proceed finishes
        Object result = aspect.aroundCacheWithTTL(joinPoint, cacheWithTTL);

        assertThat(proceedDone.await(2, TimeUnit.SECONDS)).isTrue();

        // Bug asserts: thread identity + ThreadLocals must survive
        assertThat(result).isEqualTo("result-for-acc-1");
        assertThat(proceedThread.get())
                .as("proceed() must run on caller thread, not on ForkJoinPool worker")
                .isSameAs(callerThread);
        assertThat(proceedTenant.get())
                .as("TenantContext ThreadLocal must be visible inside proceed()")
                .isEqualTo("tenant-bravo");
        assertThat(proceedPrincipal.get())
                .as("SecurityContext ThreadLocal must be visible inside proceed()")
                .isEqualTo("user-42");
    }

    static class SampleService {
        public String getAccount(String id) {
            return "account:" + id;
        }
    }
}
