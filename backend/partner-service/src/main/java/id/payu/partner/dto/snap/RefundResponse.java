package id.payu.partner.dto.snap;

public class RefundResponse {
    public String responseCode;
    public String responseMessage;
    public String partnerRefundNo;
    public String payuRefundNo;
    public String referenceNo;
    public String refundStatus;

    public RefundResponse() {
    }

    public RefundResponse(String responseCode, String responseMessage, String partnerRefundNo, 
                          String payuRefundNo, String referenceNo, String refundStatus) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.partnerRefundNo = partnerRefundNo;
        this.payuRefundNo = payuRefundNo;
        this.referenceNo = referenceNo;
        this.refundStatus = refundStatus;
    }
}
