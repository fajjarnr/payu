package id.payu.auth.domain.model;

public record LoginContext(
    String username,
    String ipAddress,
    String deviceId,
    String userAgent,
    Long timestamp
) {}
