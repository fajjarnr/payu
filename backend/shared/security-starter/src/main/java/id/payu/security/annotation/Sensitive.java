package id.payu.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields as sensitive data that should be masked in logs.
 *
 * <p>This annotation provides declarative PII (Personally Identifiable Information) masking
 * for fields in entities, DTOs, and other classes. When a field is annotated with {@code @Sensitive},
 * the {@link id.payu.security.masking.DataMaskingAspect} will automatically mask its value
 * when logging method arguments, return values, or any object containing this field.</p>
 *
 * <h3>Supported Field Types:</h3>
 * <ul>
 *   <li><b>Personal Identifiable Information:</b> NIK, phone number, email, full name</li>
 *   <li><b>Financial Information:</b> Account number, card number, balance, amounts</li>
 *   <li><b>Authentication Data:</b> PIN, password, OTP, tokens</li>
 *   <li><b>Address Data:</b> Full address, ID card address</li>
 * </ul>
 *
 * <h3>Masking Behavior:</h3>
 * <ul>
 *   <li><b>NIK (16 digits):</b> First 4 + last 4 visible (e.g., 3201********0001)</li>
 *   <li><b>Phone Number:</b> Country code + masked middle + last 4 (e.g., +62****1234)</li>
 *   <li><b>Email:</b> First char + masked + domain (e.g., u***@example.com)</li>
 *   <li><b>Account Number:</b> Last 4 digits only (e.g., ****1234)</li>
 *   <li><b>Card Number:</b> First 4 + last 4 (e.g., 1234********5678)</li>
 *   <li><b>Password/PIN/Token:</b> Fully masked (e.g., ****)</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * public class UserEntity {
 *
 *     @Sensitive
 *     @Column(name = "nik", nullable = false)
 *     private String nik;
 *
 *     @Sensitive
 *     @Column(name = "phone_number")
 *     private String phoneNumber;
 *
 *     @Sensitive
 *     @Column(name = "email")
 *     private String email;
 *
 *     @Column(name = "full_name")
 *     private String fullName; // Not sensitive in all contexts
 * }
 *
 * public record LoginRequest(
 *     @Sensitive String username,
 *     @Sensitive String password
 * ) {}
 * }</pre>
 *
 * <h3>Compliance:</h3>
 * This annotation helps ensure compliance with:
 * <ul>
 *   <li>Indonesian UU PDP (Personal Data Protection Law)</li>
 *   <li>PCI-DSS for payment card data</li>
 *   <li>OJK regulations for financial data</li>
 * </ul>
 *
 * @see id.payu.security.masking.DataMaskingAspect
 * @see id.payu.security.config.SecurityProperties
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    /**
     * Optional sensitivity level for different masking strategies.
     * Default is STANDARD which applies standard masking patterns.
     */
    SensitivityLevel value() default SensitivityLevel.STANDARD;

    /**
     * Sensitivity levels determining the masking strategy.
     */
}
