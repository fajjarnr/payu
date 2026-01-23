package id.payu.portal.dto;

import java.util.Map;

public record AggregatedOpenApiResponse(
    String version,
    Map<String, OpenApiSpec> services,
    long lastUpdated
) {
}
