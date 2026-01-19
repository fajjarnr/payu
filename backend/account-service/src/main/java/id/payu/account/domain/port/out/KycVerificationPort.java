package id.payu.account.domain.port.out;

import id.payu.account.dto.DukcapilResponse;

public interface KycVerificationPort {
    DukcapilResponse verifyNik(String nik, String fullName);
}
