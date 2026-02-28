package id.payu.gateway.domain.entity;

import java.time.Instant;
import java.util.*;

/**
 * Domain entity representing a request/response transformation rule.
 * <p>
 * Transformation rules allow configurable modification of HTTP requests and responses,
 * including header injection, removal, rewriting, and body field masking.
 * <p>
 * This is an aggregate root in the Transformation bounded context.
 */
public class TransformationRule {

    private final String id;
    private String name;
    private String description;
    private int priority;
    private boolean active;
    private final List<RuleCondition> conditions;
    private final List<RuleAction> actions;
    private final Instant createdAt;
    private Instant updatedAt;

    public TransformationRule(String id, String name, String description, int priority) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = description;
        this.priority = priority;
        this.active = true;
        this.conditions = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Domain behavior

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void updatePriority(int newPriority) {
        this.priority = newPriority;
        this.updatedAt = Instant.now();
    }

    public void addCondition(RuleCondition condition) {
        Objects.requireNonNull(condition, "Condition cannot be null");
        this.conditions.add(condition);
        this.updatedAt = Instant.now();
    }

    public void removeCondition(RuleCondition condition) {
        this.conditions.remove(condition);
        this.updatedAt = Instant.now();
    }

    public void addAction(RuleAction action) {
        Objects.requireNonNull(action, "Action cannot be null");
        this.actions.add(action);
        this.updatedAt = Instant.now();
    }

    public void removeAction(RuleAction action) {
        this.actions.remove(action);
        this.updatedAt = Instant.now();
    }

    /**
     * Check if this rule applies to the given context.
     */
    public boolean matches(TransformationContext context) {
        if (!active) {
            return false;
        }

        // All conditions must match (AND logic)
        for (RuleCondition condition : conditions) {
            if (!condition.evaluate(context)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Apply all actions to the given context.
     */
    public void apply(TransformationContext context) {
        for (RuleAction action : actions) {
            action.execute(context);
        }
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isActive() {
        return active;
    }

    public List<RuleCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public List<RuleAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransformationRule that = (TransformationRule) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Inner classes for conditions and actions

    /**
     * Interface for rule conditions.
     */
    public interface RuleCondition {
        boolean evaluate(TransformationContext context);
    }

    /**
     * Interface for rule actions.
     */
    public interface RuleAction {
        void execute(TransformationContext context);
    }

    /**
     * Context object passed through transformation pipeline.
     */
    public static class TransformationContext {
        private final String path;
        private final String method;
        private final Map<String, List<String>> headers;
        private String body;
        private final Map<String, Object> attributes;

        public TransformationContext(String path, String method, Map<String, List<String>> headers, String body) {
            this.path = path;
            this.method = method;
            this.headers = new HashMap<>(headers != null ? headers : Collections.emptyMap());
            this.body = body;
            this.attributes = new HashMap<>();
        }

        public String getPath() {
            return path;
        }

        public String getMethod() {
            return method;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }

        public Object getAttribute(String key) {
            return attributes.get(key);
        }
    }
}
