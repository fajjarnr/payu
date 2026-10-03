package id.payu.resilience.fallback;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface for providing fallback implementations when resilience patterns trigger.
 * Implementations should provide alternative responses or graceful degradation
 * when the primary service call fails.
 *
 * <p>This interface supports both synchronous and reactive fallback patterns.
 *
 * @param <T> the return type of the fallback
 * @see CachedFallback
 * @see StaticFallback
 */
@FunctionalInterface
public interface FallbackProvider<T> {

    /**
     * Provide a fallback response when the primary operation fails.
     */
    T provide(Exception exception);

    /**
     * Create a fallback provider that returns a static value.
     */
    static <T> FallbackProvider<T> of(T value) {
        return exception -> value;
    }

    static <T> FallbackProvider<T> fromSupplier(Supplier<T> supplier) {
        return exception -> supplier.get();
    }

    /**
     * Create a fallback provider that transforms the exception.
     */
    static <T> FallbackProvider<T> fromException(Function<Exception, T> mapper) {
        return mapper::apply;
    }

    /**
     * If this fallback throws an exception, the next fallback is tried.
     */
    default FallbackProvider<T> orElse(FallbackProvider<T> next) {
        return exception -> {
            try {
                return provide(exception);
            } catch (Exception e) {
                return next.provide(exception);
            }
        };
    }

    /**
     * If this fallback throws an exception, the static value is returned.
     */
    default FallbackProvider<T> orElse(T value) {
        return orElse(of(value));
    }
}
