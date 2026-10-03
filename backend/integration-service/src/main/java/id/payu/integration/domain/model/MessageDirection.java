package id.payu.integration.domain.model;

public enum MessageDirection {
    /**
     * Messages received from external systems
     */
    INBOUND,

    /**
     * Messages sent to external systems
     */
    OUTBOUND
}
