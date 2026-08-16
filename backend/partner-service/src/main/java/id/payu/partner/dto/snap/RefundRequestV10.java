package id.payu.partner.dto.snap;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SNAP-BI v1.0 refund request body ({@code POST /v1.0/transfer-va/refund}).
 *
 * <p>The original payment reference is carried in the body
 * ({@code originalReferenceNo}) per the SNAP-BI taxonomy, unlike the legacy
 * path-scoped {@code /v1/partner/payments/{id}/refund}.
 */
public class RefundRequestV10 {

    @JsonProperty("originalReferenceNo")
    public String originalReferenceNo;

    @JsonProperty("partnerReferenceNo")
    public String partnerReferenceNo;

    @JsonProperty("amount")
    public RefundRequest.Amount amount;

    @JsonProperty("reason")
    public String reason;

    /**
     * Converts this body to the legacy {@link RefundRequest} shape consumed by
     * {@code SnapBiPaymentService.createRefund}.
     */
    public RefundRequest toLegacy() {
        RefundRequest legacy = new RefundRequest();
        legacy.partnerRefundNo = partnerReferenceNo;
        legacy.amount = amount;
        legacy.reason = reason;
        return legacy;
    }
}
