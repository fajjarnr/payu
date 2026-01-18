package id.payu.account.dto;

public record DukcapilResponse(
    String requestId,
    String nik,
    boolean verified,
    String status,
    String responseCode,
    String responseMessage
) {}
