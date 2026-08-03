package id.payu.integration.application.port.out;

import id.payu.integration.domain.model.IntegrationMessage;

import java.util.Map;

public interface MessagePublisherPort {

    void publishToKafka(String topic, IntegrationMessage message);

    String sendHttp(String url, String payload);

    /**
     * Dispatch a message to an internal Camel route (e.g. "direct:swift-inbound").
     *
     * <p>BUG-INT-HEX-001 Fix (iter 46): extracted from direct
     * {@code ProducerTemplate.sendBody} usage in IntegrationService so application
     * layer doesn't depend on Camel API. Adapter (CamelMessagingAdapter)
     * implements this using {@code ProducerTemplate}.</p>
     *
     * @param routeId Camel route endpoint URI (e.g. "direct:swift-inbound")
     * @param message domain message to send through the route
     * @param headers optional headers (may be empty; never null)
     * @return route response body (if any); empty string otherwise
     */
    String routeInternal(String routeId, IntegrationMessage message, Map<String, Object> headers);
}
