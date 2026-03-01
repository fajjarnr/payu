package id.payu.integration.domain.model;

/**
 * Direction of message flow in the integration layer.
 */
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
