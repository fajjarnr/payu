package id.payu.notification.adapter.messaging;

import id.payu.notification.interfaces.dto.SendNotificationRequest;
import id.payu.notification.application.service.NotificationService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Artemis JMS consumer for notification commands.
 * Listens on point-to-point queue: payu.notification.commands
 */
@ApplicationScoped
public class ArtemisCommandConsumer {

    private static final Logger LOG = Logger.getLogger(ArtemisCommandConsumer.class);

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    NotificationService notificationService;

    @Inject
    ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    void onStart(@Observes StartupEvent ev) {
        executor.submit(this::listen);
    }

    void onStop(@Observes ShutdownEvent ev) {
        running = false;
        executor.shutdown();
    }

    private void listen() {
        LOG.info("Starting Artemis notification command listener...");
        while (running) {
            try (Connection connection = connectionFactory.createConnection();
                 Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
                
                connection.start();
                Destination destination = session.createQueue("payu.notification.commands");
                try (MessageConsumer consumer = session.createConsumer(destination)) {
                    LOG.info("Artemis command consumer connected to payu.notification.commands");
                    while (running) {
                        Message message = consumer.receive(1000);
                        if (message instanceof TextMessage textMessage) {
                            String text = textMessage.getText();
                            LOG.infof("Received command from Artemis: %d bytes", text.length());
                            processCommand(text);
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    LOG.error("Error in Artemis command consumer loop, retrying in 5 seconds...", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private void processCommand(String payload) {
        try {
            SendNotificationRequest request = objectMapper.readValue(payload, SendNotificationRequest.class);
            LOG.infof("Processing command to send notification to user: %s", request.userId());
            notificationService.send(request, request.idempotencyKey());
        } catch (Exception e) {
            LOG.error("Failed to process notification command from Artemis", e);
        }
    }
}
