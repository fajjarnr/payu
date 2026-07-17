package id.payu.promotion.adapter.messaging;

import id.payu.promotion.application.port.in.ProcessCashbackUseCase;
import id.payu.promotion.dto.TransactionCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for transaction completed events.
 * Triggers cashback processing when transactions are completed.
 */
@Component
public class TransactionCompletedConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionCompletedConsumer.class);

    private final ProcessCashbackUseCase cashbackProcessorService;

    public TransactionCompletedConsumer(ProcessCashbackUseCase cashbackProcessorService) {
        this.cashbackProcessorService = cashbackProcessorService;
    }

    /**
     * Consumes transaction completed events from Kafka.
     *
     * @param event the transaction completed event
     * @param acknowledgment the Kafka acknowledgment
     */
    @KafkaListener(
            topics = "${app.kafka.topics.transaction-completed:transaction.completed}",
            groupId = "${spring.kafka.consumer.group-id:promotion-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionCompleted(@Payload TransactionCompletedEvent event, Acknowledgment acknowledgment) {
        LOG.info("Received transaction completed event: transactionId={}, accountId={}",
                event.transactionId(), event.accountId());

        try {
            cashbackProcessorService.process(event);
            acknowledgment.acknowledge();
            LOG.debug("Successfully processed transaction event: {}", event.transactionId());
        } catch (Exception e) {
            LOG.error("Failed to process transaction event: {}", event.transactionId(), e);
            // Don't acknowledge - will be retried
            throw e;
        }
    }
}
