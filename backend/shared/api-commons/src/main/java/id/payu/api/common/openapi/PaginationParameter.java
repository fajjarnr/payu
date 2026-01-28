package id.payu.api.common.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for adding pagination parameters to OpenAPI operations.
 */
public final class PaginationParameter {

    private PaginationParameter() {
    }

    /**
     * Creates the page parameter.
     */
    public static Parameter pageParam() {
        return new Parameter()
                .name("page")
                .in("query")
                .description("Page number (0-based)")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.IntegerSchema()
                        ._default(BigDecimal.valueOf(0))
                        .minimum(BigDecimal.valueOf(0))
                        .example(0));
    }

    /**
     * Creates the size parameter.
     */
    public static Parameter sizeParam() {
        return new Parameter()
                .name("size")
                .in("query")
                .description("Number of items per page")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.IntegerSchema()
                        ._default(BigDecimal.valueOf(20))
                        .minimum(BigDecimal.valueOf(1))
                        .maximum(BigDecimal.valueOf(100))
                        .example(20));
    }

    /**
     * Creates the sort parameter.
     */
    public static Parameter sortParam() {
        return new Parameter()
                .name("sort")
                .in("query")
                .description("Sort field and direction (e.g., createdAt,desc)")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.StringSchema()
                        ._default("createdAt,desc")
                        .example("amount,asc"));
    }

    /**
     * Creates all pagination parameters.
     */
    public static List<Parameter> all() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(pageParam());
        parameters.add(sizeParam());
        parameters.add(sortParam());
        return parameters;
    }

    /**
     * Adds pagination parameters to an operation.
     */
    public static Operation addTo(Operation operation) {
        all().forEach(operation::addParametersItem);
        return operation;
    }
}
