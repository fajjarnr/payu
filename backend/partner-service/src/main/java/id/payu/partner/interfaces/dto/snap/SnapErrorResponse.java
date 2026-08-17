package id.payu.partner.interfaces.dto.snap;

/**
 * Standard error response for SNAP BI API endpoints.
 * Extracted from SnapBiController inner class for reusability (BUG-BE-146).
 */
public class SnapErrorResponse {
    private String responseCode;
    private String responseMessage;

    public SnapErrorResponse() {
    }

    public SnapErrorResponse(String responseCode, String responseMessage) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }
}
