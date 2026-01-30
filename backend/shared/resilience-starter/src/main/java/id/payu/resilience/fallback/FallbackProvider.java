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
     *
     * @param exception the exception that caused the fallback
     * @return the fallback response
     */
    T provide(Exception exception);

    /**
     * Create a fallback provider that returns a static value.
     *
     * @param value the static value to return
     * @param <T>   the type of the value
     * @return a fallback provider that always returns the given value
     */
    static <T> FallbackProvider<T> of(T value) {
        return exception -> value;
    }

    /**
     * Create a fallback provider from a supplier.
     *
     * @param supplier the supplier to get the fallback value
     * @param <T>     the type of the value
     * @return a fallback provider that calls the supplier
     */
    static <T> FallbackProvider<T> fromSupplier(Supplier<T> supplier) {
        return exception -> supplier.get();
    }

    /**
     * Create a fallback provider that transforms the exception.
     *
     * @param mapper the function to transform the exception into a result
     * @param <T>   the type of the result
     * @return a fallback provider that uses the mapper
     */
    static <T> FallbackProvider<T> fromException(Function<Exception, T> mapper) {
        return mapper::apply;
    }

    /**
     * Chain this fallback with another fallback.
     * If this fallback throws an exception, the next fallback is tried.
     *
     * @param next the next fallback to try
     * @return a composed fallback provider
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
     * Chain this fallback with a static value.
     * If this fallback throws an exception, the static value is returned.
     *
     * @param value the static value to return on failure
     * @return a composed fallback provider
     */
    default FallbackProvider<T> orElse(T value) {
        return orElse(of(value));
    }
}
