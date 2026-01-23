package id.payu.portal.dto;

import java.util.List;

public record ServiceListResponse(
    List<ServiceInfo> services
) {
}
