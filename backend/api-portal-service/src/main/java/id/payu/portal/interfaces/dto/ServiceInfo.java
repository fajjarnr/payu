package id.payu.portal.interfaces.dto;

import java.util.List;

public record ServiceInfo(
    String id,
    String name,
    String url,
    String openapiPath,
    String status
) {
}
