package id.payu.auth.dto;

public record LoginContext(
    String username,
    String ipAddress,
    String deviceId,
    String userAgent,
    Long timestamp
) {}
