package id.payu.integration.application.port.out;

import id.payu.integration.domain.model.IntegrationMessage;

public interface MessagePublisherPort {
    void publishToKafka(String topic, IntegrationMessage message);
    String publishToGrpc(String serviceName, String operation, String payload);
    String sendHttp(String url, String payload);
}
