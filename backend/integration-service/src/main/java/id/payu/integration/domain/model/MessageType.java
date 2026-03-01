package id.payu.integration.domain.model;

/**
 * Enumeration of supported legacy message types.
 * Defines the format and protocol for integration messages.
 */
public enum MessageType {
    /**
     * SWIFT MT103 - Single Customer Credit Transfer
     * Used for international wire transfers
     */
    SWIFT_MT103,

    /**
     * SWIFT MT202 - General Financial Institution Transfer
     * Used for bank-to-bank transfers
     */
    SWIFT_MT202,

    /**
     * SWIFT MT940 - Customer Statement Message
     * Used for end-of-day account statements
     */
    SWIFT_MT940,

    /**
     * OJK CSV format for regulatory reporting
     * Used for daily/monthly reports to OJK
     */
    OJK_CSV,

    /**
     * OJK XML format for regulatory reporting
     * Used for structured regulatory submissions
     */
    OJK_XML,

    /**
     * SOAP 1.1/1.2 Web Service calls
     * Used for legacy system integration
     */
    SOAP,

    /**
     * Generic HTTP/REST integration
     * Fallback for non-standard integrations
     */
    HTTP_JSON
}
