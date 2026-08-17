package id.payu.portal.interfaces.dto;

import java.util.Map;

public record OpenApiSpec(
    String openapi,
    Map<String, Object> info,
    Map<String, Object> paths,
    Map<String, Object> components
) {
}
