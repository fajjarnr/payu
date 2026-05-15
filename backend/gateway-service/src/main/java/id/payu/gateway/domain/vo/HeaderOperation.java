package id.payu.gateway.domain.vo;

import id.payu.gateway.domain.entity.TransformationRule.TransformationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Value Object representing a header transformation operation.
 * Immutable and thread-safe.
 */
public record HeaderOperation(Type type, String headerName, String value, String condition) {

    public HeaderOperation {
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(headerName, "Header name cannot be null");
    }

    public static HeaderOperation add(String headerName, String value) {
        return new HeaderOperation(Type.ADD, headerName, value, null);
    }

    public static HeaderOperation addIfMissing(String headerName, String value) {
        return new HeaderOperation(Type.ADD_IF_MISSING, headerName, value, null);
    }

    public static HeaderOperation remove(String headerName) {
        return new HeaderOperation(Type.REMOVE, headerName, null, null);
    }

    public static HeaderOperation rewrite(String headerName, String newValue) {
        return new HeaderOperation(Type.REWRITE, headerName, newValue, null);
    }

    public static HeaderOperation conditionalAdd(String headerName, String value, String condition) {
        return new HeaderOperation(Type.ADD, headerName, value, condition);
    }

    /**
     * Apply this operation to the given context.
     */
    public void apply(TransformationContext context) {
        switch (type) {
            case ADD -> {
                List<String> values = context.getHeaders().computeIfAbsent(headerName, k -> new ArrayList<>());
                if (value != null) {
                    values.add(value);
                }
            }
            case ADD_IF_MISSING -> {
                if (!context.getHeaders().containsKey(headerName) && value != null) {
                    List<String> values = new ArrayList<>();
                    values.add(value);
                    context.getHeaders().put(headerName, values);
                }
            }
            case REMOVE -> context.getHeaders().remove(headerName);
            case REWRITE -> {
                if (value != null) {
                    List<String> values = new ArrayList<>();
                    values.add(value);
                    context.getHeaders().put(headerName, values);
                }
            }
        }
    }
}
