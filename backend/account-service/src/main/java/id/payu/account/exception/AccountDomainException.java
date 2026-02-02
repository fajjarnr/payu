package id.payu.account.exception;

import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.exception.ConflictException;
import id.payu.api.common.exception.ExternalServiceException;

/**
 * Base exception for Account Service domain errors.
 *
 * Error Code Structure: ACCT_[CATEGORY]_[SPECIFIC]
 *
 * Categories:
 * - VAL: Validation errors
 * - AUTH: Authentication/authorization errors
 * - BUS: Business rule violations
 * - EXT: External service errors
 * - SYS: System/technical errors
 */
public abstract class AccountDomainException extends BusinessException {

    protected AccountDomainException(String code, String message) {
        super(code, message);
    }

    protected AccountDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    protected AccountDomainException(String code, String message, Object... args) {
        super(code, message, args);
    }

    // === Validation Errors (5000-5099) ===

    public static class InvalidPhoneNumberException extends AccountDomainException {
        public InvalidPhoneNumberException(String phone) {
            super("ACCT_VAL_001",
                  "Invalid phone number format: " + phone,
                  "Nomor telepon tidak valid");
        }
    }

    public static class InvalidEmailException extends AccountDomainException {
        public InvalidEmailException(String email) {
            super("ACCT_VAL_002",
                  "Invalid email format: " + email,
                  "Format email tidak valid");
        }
    }

    public static class InvalidNikException extends AccountDomainException {
        public InvalidNikException(String nik) {
            super("ACCT_VAL_003",
                  "Invalid NIK format: " + nik,
                  "Format NIK tidak valid");
        }
    }

    // === Business Rule Errors (5100-5199) ===

    public static class AccountAlreadyExistsException extends ConflictException {
        public AccountAlreadyExistsException(String identifier) {
            super("ACCT_BUS_001",
                  "Account already exists with identifier: " + identifier);
        }
    }

    public static class AccountNotActiveException extends AccountDomainException {
        public AccountNotActiveException(String accountId) {
            super("ACCT_BUS_002",
                  "Account is not active: " + accountId,
                  "Akun tidak aktif");
        }
    }

    public static class KycNotVerifiedException extends AccountDomainException {
        public KycNotVerifiedException(String accountId) {
            super("ACCT_BUS_003",
                  "KYC not verified for account: " + accountId,
                  "Verifikasi identitas belum selesai");
        }
    }

    public static class AccountBlockedException extends AccountDomainException {
        public AccountBlockedException(String accountId) {
            super("ACCT_BUS_004",
                  "Account is blocked: " + accountId,
                  "Akun diblokir, silakan hubungi customer service");
        }
    }

    // === External Service Errors (5200-5299) ===

    public static class DukcapilVerificationFailedException extends ExternalServiceException {
        public DukcapilVerificationFailedException(String reason) {
            super("ACCT_EXT_001",
                  "Dukcapil verification failed: " + reason,
                  "Verifikasi data kependudukan gagal",
                  "Dukcapil");
        }

        public DukcapilVerificationFailedException(String reason, Throwable cause) {
            super("ACCT_EXT_001",
                  "Dukcapil verification failed: " + reason,
                  cause,
                  "Dukcapil");
        }
    }

    public static class DukcapilServiceUnavailableException extends ExternalServiceException {
        public DukcapilServiceUnavailableException() {
            super("ACCT_EXT_002",
                  "Dukcapil service is unavailable",
                  "Layanan verifikasi sedang tidak tersedia, silakan coba lagi",
                  "Dukcapil");
        }

        public DukcapilServiceUnavailableException(Throwable cause) {
            super("ACCT_EXT_002",
                  "Dukcapil service is unavailable",
                  cause,
                  "Dukcapil");
        }
    }

    // === System Errors (5900-5999) ===

    public static class AccountCreationFailedException extends AccountDomainException {
        public AccountCreationFailedException(String reason) {
            super("ACCT_SYS_001",
                  "Failed to create account: " + reason,
                  "Gagal membuat akun, silakan coba lagi");
        }

        public AccountCreationFailedException(String reason, Throwable cause) {
            super("ACCT_SYS_001",
                  "Failed to create account: " + reason,
                  cause);
        }
    }
}
