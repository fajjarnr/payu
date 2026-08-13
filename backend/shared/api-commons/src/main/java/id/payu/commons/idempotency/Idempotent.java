package id.payu.commons.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as idempotent, enabling automatic idempotency handling.
 * <p>
 * When this annotation is applied to a method, the {@link IdempotencyInterceptor}
 * will automatically:
 * <ul>
 *   <li>Extract the Idempotency-Key header from the request</li>
 *   <li>Check for existing cached responses</li>
 *   <li>Return cached response for duplicate requests</li>
 *   <li>Store the response for future duplicate detection</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>
 * &#64;RestController
 * &#64;RequestMapping("/api/v1/transfers")
 * public class TransferController {
 *
 *     &#64;PostMapping
 *     &#64;Idempotent(required = true)  // X-Idempotency-Key header is mandatory
 *     public ResponseEntity&lt;TransferResponse&gt; createTransfer(
 *             &#64;RequestBody TransferRequest request) {
 *         // Method implementation
 *         // X-Idempotency-Key header is handled automatically
 *     }
 * }
 * </pre>
 * <p>
 * The annotation can be applied at both class and method level. Method-level
 * annotations override class-level settings.
 *
 * @see IdempotencyInterceptor
 * @see IdempotencyService
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Specifies whether the Idempotency-Key header is required.
     * <p>
     * Default is true (ARCH-IDM-001): every money mutation endpoint must
     * carry the X-Idempotency-Key header.
     *
     * @return true if Idempotency-Key header is mandatory
     */
    boolean required() default true;

    /**
     * Specifies the TTL (time-to-live) for the idempotency entry in hours.
     * <p>
     * After this period, the entry will be automatically cleaned up.
     *
     * @return TTL in hours (default: 24)
     */
    int ttlHours() default 24;

    /**
     * Specifies the header name for the idempotency key.
     * Platform standard (ARCH-IDM-001): "X-Idempotency-Key".
     *
     * @return the header name
     */
    String headerName() default "X-Idempotency-Key";

    /**
     * Specifies which HTTP methods should be considered for idempotency.
     * <p>
     * By default, only POST, PUT, and PATCH methods are idempotent-enabled
     * as GET and DELETE should inherently be idempotent.
     *
     * @return array of HTTP methods to apply idempotency
     */
    String[] methods() default {"POST", "PUT", "PATCH"};

    /**
     * Specifies whether to store error responses for idempotency.
     * <p>
     * If true, error responses (4xx, 5xx) will be cached and returned
     * for duplicate requests. If false, only successful responses are cached.
     *
     * @return true to cache error responses
     */
    boolean cacheErrors() default true;
}
