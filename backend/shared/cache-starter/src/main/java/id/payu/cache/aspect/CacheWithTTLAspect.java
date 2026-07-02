package id.payu.cache.aspect;

import id.payu.cache.annotation.CacheInvalidate;
import id.payu.cache.annotation.CacheWithTTL;
import id.payu.cache.model.CacheEntry;
import id.payu.cache.service.CacheService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aspect for implementing @CacheWithTTL and @CacheInvalidate annotations.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic cache key generation with SpEL support</li>
 *   <li>Stale-while-revalidate pattern</li>
 *   <li>Conditional caching based on SpEL expressions</li>
 *   <li>Sync access to prevent cache stampede</li>
 *   <li>Async refresh background processing</li>
 * </ul>
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class CacheWithTTLAspect {

    private final CacheService cacheService;
    private final Executor cacheRefreshExecutor;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    // In-flight requests tracker for sync access
    private final ConcurrentReferenceHashMap<String, CompletableFuture<?>> inFlightRequests =
            new ConcurrentReferenceHashMap<>();

    // Per-key monitor objects for stampede protection (GAP-27: kept on caller thread to preserve ThreadLocals)
    private final ConcurrentHashMap<String, Object> syncLocks = new ConcurrentHashMap<>();

    @Around("@annotation(cacheWithTTL)")
    public Object aroundCacheWithTTL(ProceedingJoinPoint joinPoint, CacheWithTTL cacheWithTTL) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Check condition
        if (StringUtils.isNotBlank(cacheWithTTL.condition())) {
            if (!evaluateCondition(cacheWithTTL.condition(), method, joinPoint.getArgs(), joinPoint.getTarget())) {
                log.debug("Cache condition false, bypassing cache for method: {}", method.getName());
                return joinPoint.proceed();
            }
        }

        // Generate cache key
        String cacheKey = generateCacheKey(cacheWithTTL.cacheName(), cacheWithTTL.key(), method, joinPoint.getArgs());

        // Calculate TTLs
        long ttlSeconds = cacheWithTTL.timeUnit().toSeconds(cacheWithTTL.ttl());
        long softTtlSeconds = (long) (ttlSeconds * cacheWithTTL.softTtlMultiplier());
        long hardTtlSeconds = ttlSeconds;

        if (cacheWithTTL.staleWhileRevalidate()) {
            return handleStaleWhileRevalidate(
                    joinPoint, cacheWithTTL, cacheKey, softTtlSeconds, hardTtlSeconds);
        } else {
            return handleSimpleCache(joinPoint, cacheWithTTL, cacheKey, ttlSeconds);
        }
    }

    @Around("@annotation(cacheInvalidate)")
    public Object aroundCacheInvalidate(ProceedingJoinPoint joinPoint, CacheInvalidate cacheInvalidate) throws Throwable {
        Object result = joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (cacheInvalidate.allEntries()) {
            // Note: This would require a different approach to clear all entries
            // For now, we just log
            log.warn("Cache invalidation for all entries requested for: {} - not implemented", cacheInvalidate.cacheName());
        } else {
            String cacheKey = generateCacheKey(cacheInvalidate.cacheName(), cacheInvalidate.key(), method, joinPoint.getArgs());
            cacheService.invalidate(cacheKey);
            log.debug("Invalidated cache entry: {}", cacheKey);
        }

        return result;
    }

    private Object handleSimpleCache(
            ProceedingJoinPoint joinPoint,
            CacheWithTTL cacheWithTTL,
            String cacheKey,
            long ttlSeconds) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        if (cacheWithTTL.sync()) {
            return handleSyncCache(joinPoint, cacheWithTTL, cacheKey, ttlSeconds, returnType);
        } else {
            return handleAsyncCache(joinPoint, cacheWithTTL, cacheKey, ttlSeconds, returnType);
        }
    }

    private Object handleSyncCache(
            ProceedingJoinPoint joinPoint,
            CacheWithTTL cacheWithTTL,
            String cacheKey,
            long ttlSeconds,
            Class<?> returnType) throws Throwable {

        Timer.Sample sample = Timer.start(Metrics.globalRegistry);

        // Fast path: cache hit without locking
        Object cachedValue = cacheService.get(cacheKey, returnType);
        if (cachedValue != null) {
            Metrics.counter("cache.aspect.hit", "cache", cacheWithTTL.cacheName()).increment();
            sample.stop(Timer.builder("cache.aspect.latency")
                .tag("cache", cacheWithTTL.cacheName())
                .tag("result", "hit")
                .register(Metrics.globalRegistry));
            log.debug("Cache hit for key: {}", cacheKey);
            return cachedValue;
        }

        Metrics.counter("cache.aspect.miss", "cache", cacheWithTTL.cacheName()).increment();
        log.debug("Cache miss for key: {}", cacheKey);

        // GAP-27: stampede protection via per-key monitor, executed on caller thread.
        // Previous CompletableFuture.supplyAsync detached execution from the request thread,
        // stripping SecurityContextHolder, TenantContext, MDC, and @Transactional boundaries.
        Object lock = syncLocks.computeIfAbsent(cacheKey, k -> new Object());
        synchronized (lock) {
            // Double-check: another thread may have populated the cache while we waited for the lock
            cachedValue = cacheService.get(cacheKey, returnType);
            if (cachedValue != null) {
                Metrics.counter("cache.aspect.hit", "cache", cacheWithTTL.cacheName()).increment();
                sample.stop(Timer.builder("cache.aspect.latency")
                    .tag("cache", cacheWithTTL.cacheName())
                    .tag("result", "hit_lock")
                    .register(Metrics.globalRegistry));
                log.debug("Cache hit after lock acquisition for key: {}", cacheKey);
                return cachedValue;
            }

            try {
                Object result = joinPoint.proceed();

                // Check unless condition
                if (StringUtils.isNotBlank(cacheWithTTL.unless())) {
                    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                    Method method = signature.getMethod();
                    if (evaluateCondition(cacheWithTTL.unless(), method, joinPoint.getArgs(), joinPoint.getTarget())) {
                        log.debug("Cache unless condition true, not caching: {}", cacheKey);
                        sample.stop(Timer.builder("cache.aspect.latency")
                            .tag("cache", cacheWithTTL.cacheName())
                            .tag("result", "miss_unless")
                            .register(Metrics.globalRegistry));
                        return result;
                    }
                }

                if (result != null) {
                    cacheService.put(cacheKey, result, java.time.Duration.ofSeconds(ttlSeconds));
                    log.debug("Cached result for key: {}", cacheKey);
                }

                sample.stop(Timer.builder("cache.aspect.latency")
                    .tag("cache", cacheWithTTL.cacheName())
                    .tag("result", "miss_loaded")
                    .register(Metrics.globalRegistry));

                return result;
            } catch (Throwable e) {
                log.error("Error executing cached method: {}", e.getMessage());
                throw e;
            }
        }
    }

    private Object handleAsyncCache(
            ProceedingJoinPoint joinPoint,
            CacheWithTTL cacheWithTTL,
            String cacheKey,
            long ttlSeconds,
            Class<?> returnType) throws Throwable {

        Object cachedValue = cacheService.get(cacheKey, returnType);
        if (cachedValue != null) {
            Metrics.counter("cache.aspect.hit", "cache", cacheWithTTL.cacheName()).increment();
            return cachedValue;
        }

        Metrics.counter("cache.aspect.miss", "cache", cacheWithTTL.cacheName()).increment();
        Object result = joinPoint.proceed();

        if (result != null) {
            cacheService.put(cacheKey, result, java.time.Duration.ofSeconds(ttlSeconds));
        }

        return result;
    }

    private Object handleStaleWhileRevalidate(
            ProceedingJoinPoint joinPoint,
            CacheWithTTL cacheWithTTL,
            String cacheKey,
            long softTtlSeconds,
            long hardTtlSeconds) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        java.time.Duration softTtl = java.time.Duration.ofSeconds(softTtlSeconds);
        java.time.Duration hardTtl = java.time.Duration.ofSeconds(hardTtlSeconds);

        // Try to get from cache with stale-while-revalidate
        @SuppressWarnings("unchecked")
        Object result = cacheService.getWithStaleWhileRevalidate(
                cacheKey,
                (Class<Object>) returnType,
                () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                },
                softTtl,
                hardTtl
        );

        // Check if we need to trigger async refresh
        CacheEntry<?> entry = cacheService.getDistributedCache().getEntry(cacheKey, Object.class);
        if (entry != null && entry.isStale() && !entry.isExpired()) {
            triggerAsyncRefresh(joinPoint, cacheWithTTL, cacheKey, softTtlSeconds, hardTtlSeconds);
        }

        return result;
    }

    private void triggerAsyncRefresh(
            ProceedingJoinPoint joinPoint,
            CacheWithTTL cacheWithTTL,
            String cacheKey,
            long softTtlSeconds,
            long hardTtlSeconds) {

        CompletableFuture.runAsync(() -> {
            try {
                log.debug("Async refresh for key: {}", cacheKey);
                Object result = joinPoint.proceed();

                if (result != null) {
                    cacheService.put(
                            cacheKey,
                            result,
                            java.time.Duration.ofSeconds(softTtlSeconds),
                            java.time.Duration.ofSeconds(hardTtlSeconds)
                    );
                    Metrics.counter("cache.aspect.refresh", "cache", cacheWithTTL.cacheName()).increment();
                }
            } catch (Throwable e) {
                log.error("Error during async refresh for key {}: {}", cacheKey, e.getMessage());
                Metrics.counter("cache.aspect.refresh_error", "cache", cacheWithTTL.cacheName()).increment();
            }
        }, cacheRefreshExecutor);
    }

    private String generateCacheKey(String cacheName, String keyExpression, Method method, Object[] args) {
        if (StringUtils.isNotBlank(keyExpression)) {
            // Use custom SpEL expression
            EvaluationContext context = createEvaluationContext(method, args, null);
            Expression expression = parser.parseExpression(keyExpression);
            String key = expression.getValue(context, String.class);
            return cacheName + ":" + key;
        } else {
            // Generate default key from class name, method name, and deep hash of arguments
            // Uses Arrays.deepHashCode for collision resistance instead of per-object hashCode
            StringBuilder keyBuilder = new StringBuilder(cacheName);
            keyBuilder.append(":");
            keyBuilder.append(method.getDeclaringClass().getSimpleName());
            keyBuilder.append(":");
            keyBuilder.append(method.getName());

            if (args != null && args.length > 0) {
                keyBuilder.append(":");
                keyBuilder.append(Arrays.deepHashCode(args));
            }

            return keyBuilder.toString();
        }
    }

    private boolean evaluateCondition(String conditionExpression, Method method, Object[] args, Object target) {
        try {
            EvaluationContext context = createEvaluationContext(method, args, target);
            Expression expression = parser.parseExpression(conditionExpression);
            Boolean result = expression.getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", e.getMessage());
            return false;
        }
    }

    private EvaluationContext createEvaluationContext(Method method, Object[] args, Object target) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("method", method);
        context.setVariable("target", target);

        if (args != null) {
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            if (parameterNames != null) {
                for (int i = 0; i < args.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
        }

        return context;
    }
}
