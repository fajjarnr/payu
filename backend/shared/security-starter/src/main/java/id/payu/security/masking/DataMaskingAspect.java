package id.payu.security.masking;

import id.payu.security.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aspect for masking sensitive data in method arguments and return values
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

        // Check if it's a phone number
        if (value.matches("\\d+")) {
            if (value.length() >= 10 && value.length() <= 15) {
                return maskPhone(value);
            }
            // Check if it's a card number (16 digits)
            if (value.length() == 16) {
                return maskCard(value);
            }
            // Check if it's an account number (10+ digits)
            if (value.length() >= 10) {
                return maskAccount(value);
            }
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
            return matcher.group(1) + "******";
        }
        return maskGeneric(account);
    }

    private String maskGeneric(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 4) + "****";
    }

    private String maskObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        // Use reflection to mask sensitive fields
        try {
            StringBuilder sb = new StringBuilder(obj.getClass().getSimpleName()).append("{");

            java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                String fieldName = fields[i].getName();
                Object fieldValue = fields[i].get(obj);

                // Check if field should be masked
                boolean shouldMask = properties.getMasking().getFields().stream()
                        .anyMatch(fieldName::equalsIgnoreCase);

                if (shouldMask && fieldValue != null) {
                    sb.append(fieldName).append("=****");
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
        }
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

        switch (fieldType.toLowerCase()) {
            case "email":
                return new DataMaskingAspect(new SecurityProperties()).maskEmail(value);
            case "phone":
                return new DataMaskingAspect(new SecurityProperties()).maskPhone(value);
            case "card":
                return new DataMaskingAspect(new SecurityProperties()).maskCard(value);
            case "account":
                return new DataMaskingAspect(new SecurityProperties()).maskAccount(value);
            default:
                return new DataMaskingAspect(new SecurityProperties()).maskGeneric(value);
        }
    }
}
