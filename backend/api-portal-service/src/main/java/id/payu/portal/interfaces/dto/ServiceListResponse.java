package id.payu.portal.interfaces.dto;

import java.util.List;

public record ServiceListResponse(
    List<ServiceInfo> services
) {
}
