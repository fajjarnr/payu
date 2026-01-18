package id.payu.auth.dto;

public record LoginRequest(
    String username, 
    String password
) {}
