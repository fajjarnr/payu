package id.payu.security.audit;

import id.payu.security.annotation.Audited;
import id.payu.security.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Aspect for auditing sensitive operations
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final SecurityProperties properties;
    private final AuditLogPublisher auditLogPublisher;

    @Around("@annotation(id.payu.security.annotation.Audited)")
    public Object auditOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.getAudit().isEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited audited = method.getAnnotation(Audited.class);

        // Get HTTP request context
        HttpServletRequest request = getCurrentRequest();

        // Build audit event
        AuditEvent.AuditEventBuilder eventBuilder = AuditEvent.builder()
                .eventType(audited.operation().name())
                .operation(method.getName())
                .entityType(audited.entityType())
                .timestamp(Instant.now());

        // Extract entity ID from arguments if available
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] != null) {
            eventBuilder.entityId(extractEntityId(args[0]));
        }

        // Add HTTP context
        if (request != null) {
            eventBuilder
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .sessionId(request.getSession(false) != null ? request.getSession().getId() : null);

            // Extract user ID from request
            String userId = extractUserId(request);
            if (userId != null) {
                eventBuilder.userId(userId);
            }
        }

        // Add context data
        Map<String, Object> context = new HashMap<>();
        context.put("className", joinPoint.getTarget().getClass().getSimpleName());
        context.put("methodName", method.getName());
        eventBuilder.context(context);

        AuditEvent event = eventBuilder.build();

        try {
            // Proceed with the method execution
            Object result = joinPoint.proceed();

            // Mark as successful
            event.setSuccess(true);

            // Publish audit event
            auditLogPublisher.publishSafe(event);

            return result;
        } catch (Exception e) {
            // Mark as failed
            event.setSuccess(false);
            event.setErrorMessage(e.getMessage());

            // Publish audit event
            auditLogPublisher.publishSafe(event);

            throw e;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String extractUserId(HttpServletRequest request) {
        // Try to get from security context
        Object principal = request.getAttribute("principal");
        if (principal != null) {
            return principal.toString();
        }

        // Try from header
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        return "anonymous";
    }

    private String extractEntityId(Object arg) {
        if (arg == null) {
            return null;
        }

        try {
            // Try to get ID via reflection
            java.lang.reflect.Field idField = arg.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object id = idField.get(arg);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return arg.toString();
        }
    }
}
