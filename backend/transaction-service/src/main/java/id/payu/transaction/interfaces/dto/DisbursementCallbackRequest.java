package id.payu.transaction.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for BI-FAST disbursement callback.
 */
public class DisbursementCallbackRequest {
    public DisbursementCallbackRequest() {
    }

    public DisbursementCallbackRequest(UUID disbursementId, String status, String bankReference, String failureReason) {
        this.disbursementId = disbursementId;
        this.status = status;
        this.bankReference = bankReference;
        this.failureReason = failureReason;
    }

    public static DisbursementCallbackRequestBuilder builder() {
        return new DisbursementCallbackRequestBuilder();
    }

    public static class DisbursementCallbackRequestBuilder {
        private UUID disbursementId;
        private String status;
        private String bankReference;
        private String failureReason;

        public DisbursementCallbackRequestBuilder disbursementId(UUID disbursementId) {
            this.disbursementId = disbursementId;
            return this;
        }
        public DisbursementCallbackRequestBuilder status(String status) {
            this.status = status;
            return this;
        }
        public DisbursementCallbackRequestBuilder bankReference(String bankReference) {
            this.bankReference = bankReference;
            return this;
        }
        public DisbursementCallbackRequestBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public DisbursementCallbackRequest build() {
            return new DisbursementCallbackRequest(disbursementId, status, bankReference, failureReason);
        }
    }

    public UUID getDisbursementId() {
        return disbursementId;
    }

    public void setDisbursementId(UUID disbursementId) {
        this.disbursementId = disbursementId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }



    @NotNull(message = "DisbursementEntity ID is required")
    private UUID disbursementId;

    @NotBlank(message = "Status is required")
    private String status; // COMPLETED or FAILED

    private String bankReference;

    private String failureReason;
}
