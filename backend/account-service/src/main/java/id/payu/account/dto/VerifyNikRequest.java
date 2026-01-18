package id.payu.account.dto;

import java.time.LocalDate;

public record VerifyNikRequest(
    String nik,
    String fullName,
    String birthPlace,
    String birthDate
) {}
