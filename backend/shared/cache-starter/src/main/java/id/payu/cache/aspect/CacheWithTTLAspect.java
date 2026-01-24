package id.payu.cache.aspect;

import id.payu.cache.annotation.CacheInvalidate;
import id.payu.cache.annotation.CacheWithTTL;
import id.payu.cache.model.CacheEntry;
import id.payu.cache.service.CacheService;
import io.micrometer.core.instrument.Metrics;
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
import java.util.concurrent.CompletableFuture;
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

        // Check if there's already an in-flight request for this key
        CompletableFuture<?> inFlight = inFlightRequests.get(cacheKey);
        if (inFlight != null) {
            log.debug("Waiting for in-flight request for key: {}", cacheKey);
            try {
                return inFlight.get();
            } catch (Exception e) {
                log.error("Error waiting for in-flight request: {}", e.getMessage());
                // Fall through to execute the method
            }
        }

        // Try to get from cache
        Object cachedValue = cacheService.get(cacheKey, returnType);
        if (cachedValue != null) {
            Metrics.counter("cache.aspect.hit", "cache", cacheWithTTL.cacheName()).increment();
            log.debug("Cache hit for key: {}", cacheKey);
            return cachedValue;
        }

        // Create future for this request
        Metrics.counter("cache.aspect.miss", "cache", cacheWithTTL.cacheName()).increment();
        log.debug("Cache miss for key: {}", cacheKey);

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                Object result = joinPoint.proceed();

                // Check unless condition
                if (StringUtils.isNotBlank(cacheWithTTL.unless())) {
                    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                    Method method = signature.getMethod();
                    if (evaluateCondition(cacheWithTTL.unless(), method, joinPoint.getArgs(), joinPoint.getTarget())) {
                        log.debug("Cache unless condition true, not caching: {}", cacheKey);
                        return result;
                    }
                }

                if (result != null) {
                    cacheService.put(cacheKey, result, java.time.Duration.ofSeconds(ttlSeconds));
                    log.debug("Cached result for key: {}", cacheKey);
                }

                return result;
            } catch (Throwable e) {
                log.error("Error executing cached method: {}", e.getMessage());
                throw new RuntimeException(e);
            } finally {
                inFlightRequests.remove(cacheKey);
            }
        });

        inFlightRequests.put(cacheKey, future);
        return future.get();
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
            // Generate default key from method signature and parameters
            StringBuilder keyBuilder = new StringBuilder(cacheName);
            keyBuilder.append(":");
            keyBuilder.append(method.getName());

            if (args != null && args.length > 0) {
                keyBuilder.append(":");
                for (Object arg : args) {
                    if (arg != null) {
                        keyBuilder.append(arg.hashCode()).append("_");
                    }
                }
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
