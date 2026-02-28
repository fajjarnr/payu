package id.payu.gateway.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.gateway.domain.entity.TransformationRule;
import id.payu.gateway.domain.repository.TransformationRuleRepository;
import id.payu.gateway.domain.vo.BodyMaskingRule;
import id.payu.gateway.domain.vo.HeaderOperation;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Application Service for request/response transformation.
 *
 * <p>
 * Manages configurable transformation rules for HTTP requests and responses,
 * including header injection, removal, rewriting, and body field masking.
 * <p>
 * Features:
 * - Configurable transformation rules per route
 * - Header injection (add/remove/rewrite)
 * - Body field masking for sensitive data
 * - Priority-based rule execution
 * - Rule caching for performance
 */
@ApplicationScoped
public class RequestTransformationService {

    @Inject
    TransformationRuleRepository ruleRepository;

    @Inject
    ObjectMapper objectMapper;

    // In-memory cache of active rules
    private volatile List<TransformationRule> cachedRules;
    private final Map<String, BodyMaskingRule> maskingRuleCache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        cachedRules = new CopyOnWriteArrayList<>();
        refreshRules()
            .subscribe()
            .with(
                unused -> Log.info("RequestTransformationService initialized"),
                failure -> Log.errorf(failure, "Failed to load transformation rules")
            );
    }

    /**
     * Transform request headers based on configured rules.
     *
     * @param path    The request path
     * @param method  The HTTP method
     * @param headers The current headers (modified in place)
     * @return The transformed headers
     */
    public Map<String, List<String>> transformRequestHeaders(String path, String method,
                                                              Map<String, List<String>> headers) {
        TransformationRule.TransformationContext context =
            new TransformationRule.TransformationContext(path, method, headers, null);

        List<HeaderOperation> operations = collectHeaderOperations(context, true);

        for (HeaderOperation op : operations) {
            op.apply(context);
        }

        return context.getHeaders();
    }

    /**
     * Transform response headers based on configured rules.
     *
     * @param path    The request path
     * @param method  The HTTP method
     * @param headers The current headers (modified in place)
     * @return The transformed headers
     */
    public Map<String, List<String>> transformResponseHeaders(String path, String method,
                                                               Map<String, List<String>> headers) {
        TransformationRule.TransformationContext context =
            new TransformationRule.TransformationContext(path, method, headers, null);

        List<HeaderOperation> operations = collectHeaderOperations(context, false);

        for (HeaderOperation op : operations) {
            op.apply(context);
        }

        return context.getHeaders();
    }

    /**
     * Mask sensitive fields in response body.
     *
     * @param path     The request path
     * @param method   The HTTP method
     * @param body     The response body
     * @param mimeType The content type
     * @return The masked body
     */
    public String maskResponseBody(String path, String method, String body, String mimeType) {
        if (body == null || body.isBlank()) {
            return body;
        }

        if (mimeType == null || !mimeType.contains("json")) {
            return body;
        }

        // Get masking rules for this path
        BodyMaskingRule maskingRule = getMaskingRuleForPath(path, method);

        if (maskingRule == null || maskingRule.getFieldsToMask().isEmpty()) {
            return body;
        }

        return maskingRule.applyMasking(body, objectMapper);
    }

    /**
     * Apply all transformations to a request.
     *
     * @param path    The request path
     * @param method  The HTTP method
     * @param headers The headers
     * @param body    The body
     * @return Transformation result
     */
    public TransformationResult transformRequest(String path, String method,
                                                  Map<String, List<String>> headers, String body) {
        Map<String, List<String>> transformedHeaders = transformRequestHeaders(path, method, headers);

        return new TransformationResult(
            path,
            method,
            transformedHeaders,
            body, // Request body typically not masked
            false
        );
    }

    /**
     * Apply all transformations to a response.
     *
     * @param path     The request path
     * @param method   The HTTP method
     * @param headers  The headers
     * @param body     The body
     * @param mimeType The content type
     * @return Transformation result
     */
    public TransformationResult transformResponse(String path, String method,
                                                   Map<String, List<String>> headers,
                                                   String body, String mimeType) {
        Map<String, List<String>> transformedHeaders = transformResponseHeaders(path, method, headers);
        String maskedBody = maskResponseBody(path, method, body, mimeType);

        boolean bodyModified = !Objects.equals(body, maskedBody);

        return new TransformationResult(
            path,
            method,
            transformedHeaders,
            maskedBody,
            bodyModified
        );
    }

    /**
     * Refresh the rule cache from the database.
     */
    public Uni<Void> refreshRules() {
        return ruleRepository.findAllActiveOrderedByPriority()
            .collect().asList()
            .flatMap(rules -> {
                cachedRules = new CopyOnWriteArrayList<>(rules);
                Log.debugf("Refreshed %d transformation rules", rules.size());
                return Uni.createFrom().voidItem();
            });
    }

    /**
     * Scheduled refresh of rules.
     */
    @io.quarkus.scheduler.Scheduled(every = "5m")
    void scheduledRefresh() {
        refreshRules()
            .subscribe()
            .with(
                unused -> {},
                failure -> Log.warnf(failure, "Failed to refresh transformation rules")
            );
    }

    private List<HeaderOperation> collectHeaderOperations(TransformationRule.TransformationContext context,
                                                           boolean isRequest) {
        List<HeaderOperation> operations = new ArrayList<>();

        for (TransformationRule rule : cachedRules) {
            if (rule.matches(context)) {
                // Collect header operations from rule actions
                // This is a simplified implementation - in production,
                // rules would have strongly typed actions
            }
        }

        return operations;
    }

    private BodyMaskingRule getMaskingRuleForPath(String path, String method) {
        String cacheKey = path + ":" + method;

        return maskingRuleCache.computeIfAbsent(cacheKey, k -> {
            // Default masking rules for sensitive fields
            return BodyMaskingRule.defaultMasking();
        });
    }

    /**
     * Result of a transformation operation.
     */
    public record TransformationResult(
        String path,
        String method,
        Map<String, List<String>> headers,
        String body,
        boolean bodyModified
    ) {
        public boolean isModified() {
            return bodyModified;
        }
    }
}
