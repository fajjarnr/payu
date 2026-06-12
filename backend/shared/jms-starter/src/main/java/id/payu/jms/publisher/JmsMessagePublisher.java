package id.payu.jms.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;

/**
 * Publisher abstraction for sending messages to JMS queues (point-to-point).
 */
@Slf4j
@RequiredArgsConstructor
public class JmsMessagePublisher {

    private final JmsTemplate jmsTemplate;

    /**
     * Send message to a queue immediately.
     */
    public void send(String queueName, Object message) {
        log.debug("Sending message to JMS queue: {} - {}", queueName, message);
        jmsTemplate.convertAndSend(queueName, message);
    }

    /**
     * Send message to a queue with a delay (ActiveMQ Artemis scheduled delivery).
     */
    public void sendWithDelay(String queueName, Object message, long delayMs) {
        log.debug("Sending message to JMS queue with delay: {} - {}ms - {}", queueName, delayMs, message);
        jmsTemplate.convertAndSend(queueName, message, messagePostProcessor -> {
            // ActiveMQ Artemis scheduled delivery header
            messagePostProcessor.setLongProperty("_AMQ_SCHED_DELIVERY", System.currentTimeMillis() + delayMs);
            return messagePostProcessor;
        });
    }
}
