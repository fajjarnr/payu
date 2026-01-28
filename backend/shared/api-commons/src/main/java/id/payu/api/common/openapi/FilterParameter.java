package id.payu.api.common.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for adding common filter parameters to OpenAPI operations.
 */
public final class FilterParameter {

    private FilterParameter() {
    }

    /**
     * Creates the search parameter.
     */
    public static Parameter searchParam(String description) {
        return new Parameter()
                .name("search")
                .in("query")
                .description(description != null ? description : "Search term for filtering results")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.StringSchema()
                        .example("john"));
    }

    /**
     * Creates the status parameter.
     */
    public static Parameter statusParam(String... values) {
        String exampleValue = values.length > 0 ? values[0] : "ACTIVE";
        return new Parameter()
                .name("status")
                .in("query")
                .description("Filter by status")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.StringSchema()
                        ._enum(List.of(values))
                        .example(exampleValue));
    }

    /**
     * Creates the startDate parameter.
     */
    public static Parameter startDateParam() {
        return new Parameter()
                .name("startDate")
                .in("query")
                .description("Filter by start date (ISO 8601 format)")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.StringSchema()
                        .format("date")
                        .example("2026-01-01"));
    }

    /**
     * Creates the endDate parameter.
     */
    public static Parameter endDateParam() {
        return new Parameter()
                .name("endDate")
                .in("query")
                .description("Filter by end date (ISO 8601 format)")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.StringSchema()
                        .format("date")
                        .example("2026-01-31"));
    }

    /**
     * Creates a date range parameter group.
     */
    public static List<Parameter> dateRangeParams() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(startDateParam());
        parameters.add(endDateParam());
        return parameters;
    }

    /**
     * Creates a custom filter parameter.
     */
    public static Parameter customParam(String name, String description, String example) {
        return new Parameter()
                .name(name)
                .in("query")
                .description(description)
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.StringSchema()
                        .example(example));
    }

    /**
     * Adds filter parameters to an operation.
     */
    public static Operation addTo(Operation operation, List<Parameter> parameters) {
        parameters.forEach(operation::addParametersItem);
        return operation;
    }
}
