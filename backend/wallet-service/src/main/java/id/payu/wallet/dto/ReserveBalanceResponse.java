package id.payu.wallet.dto;

public class ReserveBalanceResponse {
    private String reservationId;
    private String accountId;
    private String referenceId;
    private String status;

    public ReserveBalanceResponse() {
    }

    public ReserveBalanceResponse(String reservationId, String accountId, String referenceId, String status) {
        this.reservationId = reservationId;
        this.accountId = accountId;
        this.referenceId = referenceId;
        this.status = status;
    }

    public static ReserveBalanceResponseBuilder builder() {
        return new ReserveBalanceResponseBuilder();
    }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static class ReserveBalanceResponseBuilder {
        private String reservationId;
        private String accountId;
        private String referenceId;
        private String status;

        ReserveBalanceResponseBuilder() {}

        public ReserveBalanceResponseBuilder reservationId(String reservationId) { this.reservationId = reservationId; return this; }
        public ReserveBalanceResponseBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public ReserveBalanceResponseBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public ReserveBalanceResponseBuilder status(String status) { this.status = status; return this; }

        public ReserveBalanceResponse build() {
            return new ReserveBalanceResponse(reservationId, accountId, referenceId, status);
        }
    }
}
