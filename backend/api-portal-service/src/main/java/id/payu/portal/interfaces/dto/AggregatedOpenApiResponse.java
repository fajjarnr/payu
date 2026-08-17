package id.payu.portal.interfaces.dto;

import java.util.Map;

public record AggregatedOpenApiResponse(
    String version,
    Map<String, OpenApiSpec> services,
    long lastUpdated
) {
}
