package id.payu.billing.idempotency;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.*;

/**
 * Marks a JAX-RS resource method as idempotent for Quarkus applications.
 * <p>
 * When this annotation is applied to a method, the {@link IdempotencyInterceptor}
 * will automatically handle idempotency using Redis storage.
 * <p>
 * Usage example:
 * <pre>
 * &#64;Path("/api/v1/payments")
 * public class PaymentResource {
 *
 *     &#64;POST
 *     &#64;Idempotent(required = true)
 *     public Response createPayment(CreatePaymentRequest request) {
 *         // Method implementation
 *     }
 * }
 * </pre>
 */
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Specifies whether the Idempotency-Key header is required.
     * <p>
     * If true and the header is missing, a 400 Bad Request will be returned.
     * If false, idempotency is only applied when the header is present.
     *
     * @return true if Idempotency-Key header is mandatory
     */
    boolean required() default false;

    /**
     * Specifies the TTL (time-to-live) for the idempotency entry in hours.
     *
     * @return TTL in hours (default: 24)
     */
    int ttlHours() default 24;

    /**
     * Specifies the header name for the idempotency key.
     *
     * @return the header name
     */
    String headerName() default "Idempotency-Key";
}
