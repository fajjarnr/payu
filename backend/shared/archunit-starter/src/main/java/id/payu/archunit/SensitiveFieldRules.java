package id.payu.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import id.payu.security.annotation.Sensitive;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.ArchCondition.from;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * NEW-006: ArchUnit rules enforcing the {@code @Sensitive} annotation on
 * fields whose names match the canonical PayU PII / financial / auth-data
 * vocabulary (NIK, PIN, phone, email, password, OTP, token, account number,
 * card number, address, full name).
 *
 * <p>These rules close the gap reported in
 * {@code docs/roadmap/TODOS.md} (READY-012) — without an automated check,
 * developers can ship entities / DTOs / record components with raw PII
 * fields and bypass the {@code DataMaskingAspect} in
 * {@code security-starter}. That aspect already exists and is invoked from
 * the logging starter; the missing piece was a guardrail that prevents the
 * guardrail itself from being silently disabled.</p>
 *
 * <h3>Usage</h3>
 * <p>Each service's {@code ArchitectureTest} should include:</p>
 * <pre>{@code
 * SensitiveFieldRules.fieldsMatchingMustBeAnnotated()
 *     .check(classes);
 * }</pre>
 */
public final class SensitiveFieldRules {

    /**
     * Case-insensitive word boundaries. A field matches when its
     * <em>simple name</em> (camelCase or snake_case-normalised) contains
     * one of these tokens as a separate word.
     */
    public static final Set<String> SENSITIVE_TOKENS = Set.of(
        // PII
        "nik",
        "phone", "msisdn",
        "email",
        "fullname", "fullName",
        "address",
        // Financial
        "accountNumber", "accountNo",
        "cardNumber", "cardNo", "pan",
        "iban", "swift",
        "amount", "balance",
        // Auth / secret
        "pin", "password", "pwd", "passcode",
        "otp", "token", "secret", "apiKey", "apikey",
        "secretKey", "privateKey", "salt"
    );

    private SensitiveFieldRules() {
    }

    private static boolean hasSensitiveName(JavaField field) {
        if (field.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.STATIC)) {
            return false;
        }
        String simpleName = field.getName();
        String normalised = simpleName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        for (String token : SENSITIVE_TOKENS) {
            String pattern = "(^|[^a-z])" + Pattern.quote(token.toLowerCase()) + "([^a-z]|$)";
            if (normalised.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    private static final DescribedPredicate<JavaField> HAS_SENSITIVE_NAME =
        new DescribedPredicate<>("has name matching a PayU sensitive token") {
            @Override
            public boolean test(JavaField field) {
                return hasSensitiveName(field);
            }
        };

    /**
     * ArchUnit rule: any field whose name matches a {@link #SENSITIVE_TOKENS}
     * entry must be annotated with {@code @Sensitive}. Skips synthetic /
     * static / transient fields.
     */
    public static final ArchRule fieldsMatchingMustBeAnnotated() {
        return fields()
            .that().areDeclaredInClassesThat()
                .resideInAPackage("id.payu..")
            .and().areDeclaredInClassesThat()
                .resideOutsideOfPackage("id.payu.shared..")
            .and().areDeclaredInClassesThat()
                .resideOutsideOfPackage("id.payu.archunit..")
            .and(HAS_SENSITIVE_NAME)
            .should(beAnnotatedWithSensitive())
            .because("Fields whose names suggest PII / financial / auth data "
                + "(NIK, phone, email, accountNumber, cardNumber, password, OTP, "
                + "token, secret, etc.) must be annotated with @Sensitive so the "
                + "DataMaskingAspect in security-starter can mask them in logs. "
                + "(NEW-006 / READY-012)");
    }

    private static ArchCondition<JavaField> beAnnotatedWithSensitive() {
        return new ArchCondition<>("be annotated with @Sensitive") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                Field rawField = resolveField(field);
                if (rawField == null) {
                    return;
                }
                boolean hasSensitive = rawField.isAnnotationPresent(Sensitive.class)
                    || AnnotationUtils.findAnnotation(rawField, Sensitive.class) != null;
                if (!hasSensitive) {
                    events.add(new SimpleConditionEvent(field, false, String.format(
                        "Field '%s.%s' must be @Sensitive", field.getOwner().getName(), field.getName())));
                }
            }
        };
    }

    private static Field resolveField(JavaField archField) {
        try {
            Class<?> declaringClass = Class.forName(archField.getOwner().getName(), false,
                Thread.currentThread().getContextClassLoader());
            if (declaringClass == null) {
                return null;
            }
            return declaringClass.getDeclaredField(archField.getName());
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            return null;
        }
    }
}
