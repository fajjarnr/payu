package id.payu.api.common.constant;

import lombok.Getter;

/**
 * Centralized error code registry for PayU Digital Banking Platform.
 * Error codes follow the pattern: {SERVICE}_{CATEGORY}_{SPECIFIC}
 *
 * Categories:
 * - VAL: Validation errors (400 Bad Request)
 * - BUS: Business logic errors (422 Unprocessable Entity)
 * - EXT: External service failures (502 Bad Gateway)
 * - SYS: System errors (500 Internal Server Error)
 *
 * This class serves as the source of truth for error codes.
 * Run ./scripts/extract-errors.sh to generate JSON mapping for frontend/mobile.
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    // ==================== COMMON ERROR CODES ====================

    /**
     * Validation: Invalid request format or data
     */
    @Getter
    public static final ErrorInfo VALIDATION_ERROR = new ErrorInfo("VALIDATION_ERROR", "Request validation failed");

    /**
     * Validation: Missing required parameter
     */
    @Getter
    public static final ErrorInfo MISSING_PARAMETER = new ErrorInfo("MISSING_PARAMETER", "Required parameter is missing");

    /**
     * Validation: Invalid parameter value
     */
    @Getter
    public static final ErrorInfo INVALID_PARAMETER = new ErrorInfo("INVALID_PARAMETER", "Invalid parameter value");

    /**
     * Validation: Malformed JSON
     */
    @Getter
    public static final ErrorInfo INVALID_JSON = new ErrorInfo("INVALID_JSON", "Request body is malformed or unreadable");

    /**
     * Validation: Invalid JSON format
     */
    @Getter
    public static final ErrorInfo INVALID_FORMAT = new ErrorInfo("INVALID_FORMAT", "Invalid data format");

    /**
     * Not Found: Resource not found
     */
    @Getter
    public static final ErrorInfo NOT_FOUND = new ErrorInfo("NOT_FOUND", "Resource not found");

    /**
     * Conflict: Duplicate resource
     */
    @Getter
    public static final ErrorInfo CONFLICT = new ErrorInfo("CONFLICT", "Resource already exists");

    /**
     * External: Service unavailable
     */
    @Getter
    public static final ErrorInfo EXTERNAL_SERVICE_ERROR = new ErrorInfo("EXTERNAL_SERVICE_ERROR", "External service error");

    /**
     * Rate Limit: Too many requests
     */
    @Getter
    public static final ErrorInfo RATE_LIMIT_EXCEEDED = new ErrorInfo("RATE_LIMIT_EXCEEDED", "Rate limit exceeded");

    /**
     * Auth: Unauthorized access
     */
    @Getter
    public static final ErrorInfo UNAUTHORIZED = new ErrorInfo("UNAUTHORIZED", "Authentication required");

    /**
     * Auth: Access forbidden
     */
    @Getter
    public static final ErrorInfo FORBIDDEN = new ErrorInfo("FORBIDDEN", "Access denied");

    /**
     * System: Internal error
     */
    @Getter
    public static final ErrorInfo INTERNAL_ERROR = new ErrorInfo("INTERNAL_ERROR", "Internal server error");

    // ==================== AUTH SERVICE (AUTH) ====================

    @Getter
    public static final ErrorInfo AUTH_VAL_001 = new ErrorInfo("AUTH_VAL_001", "Username is required");
    @Getter
    public static final ErrorInfo AUTH_VAL_002 = new ErrorInfo("AUTH_VAL_002", "Password is required");
    @Getter
    public static final ErrorInfo AUTH_VAL_003 = new ErrorInfo("AUTH_VAL_003", "Invalid email format");
    @Getter
    public static final ErrorInfo AUTH_VAL_004 = new ErrorInfo("AUTH_VAL_004", "Invalid phone number format");
    @Getter
    public static final ErrorInfo AUTH_VAL_005 = new ErrorInfo("AUTH_VAL_005", "Invalid OTP code format");

    @Getter
    public static final ErrorInfo AUTH_BUS_001 = new ErrorInfo("AUTH_BUS_001", "Invalid credentials");
    @Getter
    public static final ErrorInfo AUTH_BUS_002 = new ErrorInfo("AUTH_BUS_002", "Account is locked");
    @Getter
    public static final ErrorInfo AUTH_BUS_003 = new ErrorInfo("AUTH_BUS_003", "Account is disabled");
    @Getter
    public static final ErrorInfo AUTH_BUS_004 = new ErrorInfo("AUTH_BUS_004", "Invalid or expired OTP");
    @Getter
    public static final ErrorInfo AUTH_BUS_005 = new ErrorInfo("AUTH_BUS_005", "MFA required for this account");
    @Getter
    public static final ErrorInfo AUTH_BUS_006 = new ErrorInfo("AUTH_BUS_006", "Session has expired");
    @Getter
    public static final ErrorInfo AUTH_BUS_007 = new ErrorInfo("AUTH_BUS_007", "Maximum login attempts exceeded");

    // ==================== ACCOUNT SERVICE (ACC) ====================

    @Getter
    public static final ErrorInfo ACC_VAL_001 = new ErrorInfo("ACC_VAL_001", "External ID is required");
    @Getter
    public static final ErrorInfo ACC_VAL_002 = new ErrorInfo("ACC_VAL_002", "Invalid NIK format");
    @Getter
    public static final ErrorInfo ACC_VAL_003 = new ErrorInfo("ACC_VAL_003", "Invalid date of birth format");
    @Getter
    public static final ErrorInfo ACC_VAL_004 = new ErrorInfo("ACC_VAL_004", "Invalid mother's maiden name format");

    @Getter
    public static final ErrorInfo ACC_BUS_001 = new ErrorInfo("ACC_BUS_001", "Account already exists");
    @Getter
    public static final ErrorInfo ACC_BUS_002 = new ErrorInfo("ACC_BUS_002", "Account not found");
    @Getter
    public static final ErrorInfo ACC_BUS_003 = new ErrorInfo("ACC_BUS_003", "Account is already active");
    @Getter
    public static final ErrorInfo ACC_BUS_004 = new ErrorInfo("ACC_BUS_004", "Account is frozen");
    @Getter
    public static final ErrorInfo ACC_BUS_005 = new ErrorInfo("ACC_BUS_005", "Account is closed");
    @Getter
    public static final ErrorInfo ACC_BUS_006 = new ErrorInfo("ACC_BUS_006", "Account is not eligible for this operation");

    // ==================== TRANSACTION SERVICE (TXN) ====================

    @Getter
    public static final ErrorInfo TXN_VAL_001 = new ErrorInfo("TXN_VAL_001", "Sender account ID is required");
    @Getter
    public static final ErrorInfo TXN_VAL_002 = new ErrorInfo("TXN_VAL_002", "Recipient account number is required");
    @Getter
    public static final ErrorInfo TXN_VAL_003 = new ErrorInfo("TXN_VAL_003", "Amount must be greater than zero");
    @Getter
    public static final ErrorInfo TXN_VAL_004 = new ErrorInfo("TXN_VAL_004", "Invalid amount format");
    @Getter
    public static final ErrorInfo TXN_VAL_005 = new ErrorInfo("TXN_VAL_005", "Invalid transaction type");
    @Getter
    public static final ErrorInfo TXN_VAL_006 = new ErrorInfo("TXN_VAL_006", "Invalid currency");
    @Getter
    public static final ErrorInfo TXN_VAL_007 = new ErrorInfo("TXN_VAL_007", "Invalid reference number format");
    @Getter
    public static final ErrorInfo TXN_VAL_008 = new ErrorInfo("TXN_VAL_008", "Account number mismatch with bank code");

    @Getter
    public static final ErrorInfo TXN_BUS_001 = new ErrorInfo("TXN_BUS_001", "Insufficient balance");
    @Getter
    public static final ErrorInfo TXN_BUS_002 = new ErrorInfo("TXN_BUS_002", "Daily transaction limit exceeded");
    @Getter
    public static final ErrorInfo TXN_BUS_003 = new ErrorInfo("TXN_BUS_003", "Monthly transaction limit exceeded");
    @Getter
    public static final ErrorInfo TXN_BUS_004 = new ErrorInfo("TXN_BUS_004", "Transaction not found");
    @Getter
    public static final ErrorInfo TXN_BUS_005 = new ErrorInfo("TXN_BUS_005", "Transaction already completed");
    @Getter
    public static final ErrorInfo TXN_BUS_006 = new ErrorInfo("TXN_BUS_006", "Transaction already failed");
    @Getter
    public static final ErrorInfo TXN_BUS_007 = new ErrorInfo("TXN_BUS_007", "Transaction already cancelled");
    @Getter
    public static final ErrorInfo TXN_BUS_008 = new ErrorInfo("TXN_BUS_008", "Cannot modify completed transaction");
    @Getter
    public static final ErrorInfo TXN_BUS_009 = new ErrorInfo("TXN_BUS_009", "Recipient account not found");
    @Getter
    public static final ErrorInfo TXN_BUS_010 = new ErrorInfo("TXN_BUS_010", "Same account transfer not allowed");

    @Getter
    public static final ErrorInfo TXN_EXT_BIFAST_001 = new ErrorInfo("TXN_EXT_BIFAST_001", "BI-FAST service unavailable");
    @Getter
    public static final ErrorInfo TXN_EXT_BIFAST_002 = new ErrorInfo("TXN_EXT_BIFAST_002", "BI-FAST transfer failed");
    @Getter
    public static final ErrorInfo TXN_EXT_BIFAST_003 = new ErrorInfo("TXN_EXT_BIFAST_003", "BI-FAST timeout");
    @Getter
    public static final ErrorInfo TXN_EXT_QRIS_001 = new ErrorInfo("TXN_EXT_QRIS_001", "QRIS service unavailable");
    @Getter
    public static final ErrorInfo TXN_EXT_QRIS_002 = new ErrorInfo("TXN_EXT_QRIS_002", "Invalid QR code");
    @Getter
    public static final ErrorInfo TXN_EXT_QRIS_003 = new ErrorInfo("TXN_EXT_QRIS_003", "QRIS payment failed");

    // ==================== WALLET SERVICE (WAL) ====================

    @Getter
    public static final ErrorInfo WAL_VAL_001 = new ErrorInfo("WAL_VAL_001", "Account ID is required");
    @Getter
    public static final ErrorInfo WAL_VAL_002 = new ErrorInfo("WAL_VAL_002", "Pocket name is required");

    @Getter
    public static final ErrorInfo WAL_BUS_001 = new ErrorInfo("WAL_BUS_001", "Insufficient wallet balance");
    @Getter
    public static final ErrorInfo WAL_BUS_002 = new ErrorInfo("WAL_BUS_002", "Pocket not found");
    @Getter
    public static final ErrorInfo WAL_BUS_003 = new ErrorInfo("WAL_BUS_003", "Maximum pockets reached");
    @Getter
    public static final ErrorInfo WAL_BUS_004 = new ErrorInfo("WAL_BUS_004", "Pocket name already exists");

    // ==================== INVESTMENT SERVICE (INV) ====================

    @Getter
    public static final ErrorInfo INV_VAL_001 = new ErrorInfo("INV_VAL_001", "Mutual fund code is required");
    @Getter
    public static final ErrorInfo INV_VAL_002 = new ErrorInfo("INV_VAL_002", "Amount below minimum investment");
    @Getter
    public static final ErrorInfo INV_VAL_003 = new ErrorInfo("INV_VAL_003", "Amount above maximum investment");

    @Getter
    public static final ErrorInfo INV_BUS_001 = new ErrorInfo("INV_BUS_001", "Mutual fund not found");
    @Getter
    public static final ErrorInfo INV_BUS_002 = new ErrorInfo("INV_BUS_002", "Insufficient units for redemption");
    @Getter
    public static final ErrorInfo INV_BUS_003 = new ErrorInfo("INV_BUS_003", "Mutual fund is not active");
    @Getter
    public static final ErrorInfo INV_BUS_004 = new ErrorInfo("INV_BUS_004", "Trading hours: Monday-Friday 08:00-17:00 WIB");

    // ==================== LENDING SERVICE (LEN) ====================

    @Getter
    public static final ErrorInfo LEN_VAL_001 = new ErrorInfo("LEN_VAL_001", "Loan amount is required");
    @Getter
    public static final ErrorInfo LEN_VAL_002 = new ErrorInfo("LEN_VAL_002", "Loan tenure is required");
    @Getter
    public static final ErrorInfo LEN_VAL_003 = new ErrorInfo("LEN_VAL_003", "Invalid loan amount range");

    @Getter
    public static final ErrorInfo LEN_BUS_001 = new ErrorInfo("LEN_BUS_001", "Loan not found");
    @Getter
    public static final ErrorInfo LEN_BUS_002 = new ErrorInfo("LEN_BUS_002", "Loan application already exists");
    @Getter
    public static final ErrorInfo LEN_BUS_003 = new ErrorInfo("LEN_BUS_003", "Insufficient credit score");
    @Getter
    public static final ErrorInfo LEN_BUS_004 = new ErrorInfo("LEN_BUS_004", "Maximum loan limit reached");
    @Getter
    public static final ErrorInfo LEN_BUS_005 = new ErrorInfo("LEN_BUS_005", "Loan is not in payable state");
    @Getter
    public static final ErrorInfo LEN_BUS_006 = new ErrorInfo("LEN_BUS_006", "Payment amount is less than minimum installment");

    // ==================== KYC SERVICE (KYC) ====================

    @Getter
    public static final ErrorInfo KYC_VAL_001 = new ErrorInfo("KYC_VAL_001", "KTP image is required");
    @Getter
    public static final ErrorInfo KYC_VAL_002 = new ErrorInfo("KYC_VAL_002", "Selfie image is required");
    @Getter
    public static final ErrorInfo KYC_VAL_003 = new ErrorInfo("KYC_VAL_003", "Invalid image format");

    @Getter
    public static final ErrorInfo KYC_BUS_001 = new ErrorInfo("KYC_BUS_001", "OCR failed: unable to read KTP");
    @Getter
    public static final ErrorInfo KYC_BUS_002 = new ErrorInfo("KYC_BUS_002", "Liveness check failed");
    @Getter
    public static final ErrorInfo KYC_BUS_003 = new ErrorInfo("KYC_BUS_003", "Face mismatch between KTP and selfie");
    @Getter
    public static final ErrorInfo KYC_BUS_004 = new ErrorInfo("KYC_BUS_004", "KYC already verified");
    @Getter
    public static final ErrorInfo KYC_BUS_005 = new ErrorInfo("KYC_BUS_005", "KYC verification in progress");

    @Getter
    public static final ErrorInfo KYC_EXT_DUKCAPIL_001 = new ErrorInfo("KYC_EXT_DUKCAPIL_001", "Dukcapil service unavailable");
    @Getter
    public static final ErrorInfo KYC_EXT_DUKCAPIL_002 = new ErrorInfo("KYC_EXT_DUKCAPIL_002", "Data not found in Dukcapil");

    // ==================== PARTNER SERVICE (PTR) ====================

    @Getter
    public static final ErrorInfo PTR_VAL_001 = new ErrorInfo("PTR_VAL_001", "Partner ID is required");
    @Getter
    public static final ErrorInfo PTR_VAL_002 = new ErrorInfo("PTR_VAL_002", "Invalid API key format");

    @Getter
    public static final ErrorInfo PTR_BUS_001 = new ErrorInfo("PTR_BUS_001", "Partner not found");
    @Getter
    public static final ErrorInfo PTR_BUS_002 = new ErrorInfo("PTR_BUS_002", "Partner is not active");
    @Getter
    public static final ErrorInfo PTR_BUS_003 = new ErrorInfo("PTR_BUS_003", "API key is invalid or expired");
    @Getter
    public static final ErrorInfo PTR_BUS_004 = new ErrorInfo("PTR_BUS_004", "Rate limit exceeded for partner");

    // ==================== FX SERVICE (FX) ====================

    @Getter
    public static final ErrorInfo FX_VAL_001 = new ErrorInfo("FX_VAL_001", "From currency is required");
    @Getter
    public static final ErrorInfo FX_VAL_002 = new ErrorInfo("FX_VAL_002", "To currency is required");
    @Getter
    public static final ErrorInfo FX_VAL_003 = new ErrorInfo("FX_VAL_003", "Invalid currency code");

    @Getter
    public static final ErrorInfo FX_BUS_001 = new ErrorInfo("FX_BUS_001", "Exchange rate not available");
    @Getter
    public static final ErrorInfo FX_BUS_002 = new ErrorInfo("FX_BUS_002", "Currency pair not supported");
    @Getter
    public static final ErrorInfo FX_BUS_003 = new ErrorInfo("FX_BUS_003", "Amount below minimum for FX transaction");

    // ==================== INNER CLASS FOR ERROR INFO ====================

    @Getter
    public static class ErrorInfo {
        private final String code;
        private final String message;

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
