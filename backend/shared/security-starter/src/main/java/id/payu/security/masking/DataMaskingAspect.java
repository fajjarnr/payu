package id.payu.security.masking;

import id.payu.security.annotation.Sensitive;
import id.payu.security.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aspect for masking sensitive data in method arguments and return values.
 *
 * <p>This aspect automatically masks fields annotated with {@link Sensitive} in:
 * <ul>
 *   <li>Method arguments (controllers, services)</li>
 *   <li>Return values</li>
 *   <li>Nested objects</li>
 *   <li>Record components</li>
 * </ul>
 *
 * <h3>Masking Strategy:</h3>
 * <ul>
 *   <li><b>STANDARD:</b> Partial visibility (email: u***@domain.com)</li>
 *   <li><b>HIGH:</b> Last 4 digits only (account: ****1234)</li>
 *   <li><b>CRITICAL:</b> Fully masked (password: ****)</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
public class DataMaskingAspect {

    private final SecurityProperties properties;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{1})[\\w.]+@([\\w.]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4,}(\\d{3})");
    private static final Pattern CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,}(\\d{4})");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("(\\d{4})\\d{6,}");

    // ThreadLocal to track visited objects and prevent infinite recursion
    private final ThreadLocal<IdentityHashMap<Object, Object>> visitedObjects = ThreadLocal.withInitial(IdentityHashMap::new);

    public DataMaskingAspect(SecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * Mask sensitive data in method arguments before logging
     */
    @Around("execution(* id.payu..service..*.*(..)) || execution(* id.payu..controller..*.*(..))")
    public Object maskSensitiveData(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isMaskingEnabled()) {
            return joinPoint.proceed();
        }

        // Clear visited objects before processing
        visitedObjects.get().clear();

        // Mask arguments before logging
        Object[] maskedArgs = maskArguments(joinPoint.getArgs());

        // Log masked arguments
        if (log.isDebugEnabled()) {
            log.debug("Executing: {} with masked args: {}", joinPoint.getSignature(),
                    formatArgs(maskedArgs));
        }

        Object result = joinPoint.proceed();

        // Mask return value before logging
        if (log.isDebugEnabled() && result != null) {
            Object maskedResult = maskValue(result);
            log.debug("Result: {}", maskedResult);
        }

        // Clean up after processing
        visitedObjects.remove();

        return result;
    }

    private Object[] maskArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }

        Object[] maskedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            maskedArgs[i] = maskValue(args[i]);
        }
        return maskedArgs;
    }

    private Object maskValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return maskString((String) value);
        }

        // Handle BigDecimal (amounts, balances)
        if (value instanceof BigDecimal) {
            return maskAmount((BigDecimal) value);
        }

        // Handle objects by converting to string and masking
        return maskObject(value);
    }

    private String maskString(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // Check if it's an email
        if (value.contains("@") && EMAIL_PATTERN.matcher(value).matches()) {
            return maskEmail(value);
        }

        // Check if it's a phone number (with optional + prefix)
        if (value.matches("^[+]?\\d{10,15}$")) {
            return maskPhone(value);
        }

        // Check if it's a card number (16 digits)
        if (value.matches("^\\d{16}$")) {
            return maskCard(value);
        }

        // Check if it's an account number (10+ digits)
        if (value.matches("^\\d{10,}$")) {
            return maskAccount(value);
        }

        // Generic masking: show first 4 chars, mask the rest
        return maskGeneric(value);
    }

    private String maskEmail(String email) {
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.matches()) {
            return matcher.group(1) + "***@" + matcher.group(2);
        }
        return maskGeneric(email);
    }

    private String maskPhone(String phone) {
        // Handle phone with country code
        if (phone.startsWith("+")) {
            String digits = phone.substring(1);
            if (digits.length() >= 7) {
                return "+" + digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
            }
        }
        Matcher matcher = PHONE_PATTERN.matcher(phone);
        if (matcher.matches()) {
            return matcher.group(1) + "****" + matcher.group(2);
        }
        return maskGeneric(phone);
    }

    private String maskCard(String card) {
        Matcher matcher = CARD_PATTERN.matcher(card);
        if (matcher.matches()) {
            return matcher.group(1) + "********" + matcher.group(2);
        }
        return maskGeneric(card);
    }

    private String maskAccount(String account) {
        Matcher matcher = ACCOUNT_PATTERN.matcher(account);
        if (matcher.matches()) {
            return "****" + account.substring(account.length() - 4);
        }
        return maskGeneric(account);
    }

    private String maskGeneric(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, Math.min(4, value.length())) + "****";
    }

    private String maskAmount(BigDecimal amount) {
        return "****";
    }

    private String maskObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        // Check for circular reference
        IdentityHashMap<Object, Object> visited = visitedObjects.get();
        if (visited.containsKey(obj)) {
            return obj.getClass().getSimpleName() + "[...]";
        }
        visited.put(obj, obj);

        try {
            // Use reflection to mask sensitive fields
            StringBuilder sb = new StringBuilder(obj.getClass().getSimpleName()).append("{");

            // Get all fields including inherited ones
            Field[] fields = getAllFields(obj.getClass());
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                String fieldName = fields[i].getName();

                // Get field value
                Object fieldValue;
                try {
                    fieldValue = fields[i].get(obj);
                } catch (IllegalAccessException e) {
                    sb.append(fieldName).append("=[ACCESS_DENIED]");
                    continue;
                }

                // Check if field has @Sensitive annotation
                Sensitive sensitiveAnnotation = fields[i].getAnnotation(Sensitive.class);
                boolean isFieldSensitive = sensitiveAnnotation != null;

                // Check if field name is in masking properties configuration
                boolean isConfiguredMasked = properties.getMasking().getFields().stream()
                        .anyMatch(fieldName::equalsIgnoreCase);

                boolean shouldMask = isFieldSensitive || isConfiguredMasked;

                // Apply masking based on sensitivity level
                if (shouldMask && fieldValue != null) {
                    sb.append(fieldName).append("=").append(maskFieldBySensitivity(fieldValue, sensitiveAnnotation));
                } else if (fieldValue != null) {
                    sb.append(fieldName).append("=").append(maskValue(fieldValue));
                } else {
                    sb.append(fieldName).append("=null");
                }

                if (i < fields.length - 1) {
                    sb.append(", ");
                }
            }

            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to mask object: {}", obj.getClass().getSimpleName(), e);
            return obj.getClass().getSimpleName() + "[MASKED]";
        } finally {
            // Clean up to prevent memory leaks
            visited.remove(obj);
        }
    }

    /**
     * Get all fields including inherited ones from parent classes
     */
    private Field[] getAllFields(Class<?> clazz) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(java.util.Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * Mask field value based on its @Sensitive annotation level
     */
    private String maskFieldBySensitivity(Object value, Sensitive annotation) {
        if (annotation == null) {
            // Use default masking for fields configured in properties
            return "****";
        }

        Sensitive.SensitivityLevel level = annotation.value();

        // CRITICAL level: always fully mask
        if (level == Sensitive.SensitivityLevel.CRITICAL) {
            return "****";
        }

        // HIGH level: show partial (last 4 for strings, **** for amounts)
        if (level == Sensitive.SensitivityLevel.HIGH) {
            if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.length() <= 4) {
                    return "****";
                }
                return "****" + strValue.substring(strValue.length() - 4);
            }
            return "****";
        }

        // STANDARD level: use intelligent masking based on content
        if (value instanceof String) {
            return maskString((String) value);
        }
        return "****";
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            sb.append(maskValue(args[i]));
            if (i < args.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Public method to mask a value for logging
     */
    public static String mask(String value, String fieldType) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        DataMaskingAspect aspect = new DataMaskingAspect(new SecurityProperties());
        switch (fieldType.toLowerCase()) {
            case "email":
                return aspect.maskEmail(value);
            case "phone":
                return aspect.maskPhone(value);
            case "card":
                return aspect.maskCard(value);
            case "account":
                return aspect.maskAccount(value);
            default:
                return aspect.maskGeneric(value);
        }
    }
}
