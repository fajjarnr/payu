package id.payu.portal.dto;

import java.util.Map;

public record OpenApiSpec(
    String openapi,
    Map<String, Object> info,
    Map<String, Object> paths,
    Map<String, Object> components
) {
}
