package id.payu.partner.dto;

import jakarta.validation.constraints.NotBlank;

public class CertificateRequest {
    @NotBlank
    public String certificatePem;

    @NotBlank
    public String privateKeyPem;

    public CertificateRequest() {
    }

    public CertificateRequest(String certificatePem, String privateKeyPem) {
        this.certificatePem = certificatePem;
        this.privateKeyPem = privateKeyPem;
    }
}
