package id.payu.jms.publisher;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JmsMessagePublisher Unit Tests")
class JmsMessagePublisherTest {

    @Mock
    private JmsTemplate jmsTemplate;

    private JmsMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new JmsMessagePublisher(jmsTemplate);
    }

    @Test
    @DisplayName("send should delegate to jmsTemplate.convertAndSend")
    void sendShouldDelegateToJmsTemplate() {
        publisher.send("payu.test.queue", "hello");

        verify(jmsTemplate).convertAndSend(eq("payu.test.queue"), eq("hello"));
    }

    @Test
    @DisplayName("sendWithDelay should set _AMQ_SCHED_DELIVERY to currentTime + delayMs")
    void sendWithDelayShouldSetScheduledDelivery() throws JMSException {
        long delayMs = 300000L; // 5 min
        long beforeCall = System.currentTimeMillis();

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        publisher.sendWithDelay("payu.billing.scheduled", "sub-id-123", delayMs);

        verify(jmsTemplate).convertAndSend(eq("payu.billing.scheduled"), eq("sub-id-123"), captor.capture());

        // Simulate the MessagePostProcessor
        jakarta.jms.Message mockMessage = mock(jakarta.jms.Message.class);
        captor.getValue().postProcessMessage(mockMessage);

        long afterCall = System.currentTimeMillis();

        ArgumentCaptor<Long> scheduledTimeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMessage).setLongProperty(eq("_AMQ_SCHED_DELIVERY"), scheduledTimeCaptor.capture());

        long scheduledTime = scheduledTimeCaptor.getValue();
        // Scheduled time should be approximately currentTime + delayMs
        assertThat(scheduledTime).isGreaterThanOrEqualTo(beforeCall + delayMs);
        assertThat(scheduledTime).isLessThanOrEqualTo(afterCall + delayMs + 100); // +100ms tolerance
    }

    @Test
    @DisplayName("sendWithDelay with zero delay should schedule immediately")
    void sendWithDelayZeroDelayShouldScheduleImmediately() throws JMSException {
        long beforeCall = System.currentTimeMillis();

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        publisher.sendWithDelay("payu.billing.scheduled", "immediate-sub", 0L);

        verify(jmsTemplate).convertAndSend(eq("payu.billing.scheduled"), eq("immediate-sub"), captor.capture());

        jakarta.jms.Message mockMessage = mock(jakarta.jms.Message.class);
        captor.getValue().postProcessMessage(mockMessage);

        ArgumentCaptor<Long> scheduledTimeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMessage).setLongProperty(eq("_AMQ_SCHED_DELIVERY"), scheduledTimeCaptor.capture());

        long afterCall = System.currentTimeMillis();
        long scheduledTime = scheduledTimeCaptor.getValue();

        // With 0 delay, scheduled time should be very close to current time
        assertThat(scheduledTime).isBetween(beforeCall, afterCall + 100);
    }

    @Test
    @DisplayName("sendWithDelay should handle large delay values")
    void sendWithDelayShouldHandleLargeDelay() throws JMSException {
        long delayMs = 86_400_000L; // 24 hours
        long beforeCall = System.currentTimeMillis();

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        publisher.sendWithDelay("payu.billing.scheduled", "tomorrow-sub", delayMs);

        verify(jmsTemplate).convertAndSend(eq("payu.billing.scheduled"), eq("tomorrow-sub"), captor.capture());

        jakarta.jms.Message mockMessage = mock(jakarta.jms.Message.class);
        captor.getValue().postProcessMessage(mockMessage);

        ArgumentCaptor<Long> scheduledTimeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMessage).setLongProperty(eq("_AMQ_SCHED_DELIVERY"), scheduledTimeCaptor.capture());

        // Should be roughly 24h from now
        assertThat(scheduledTimeCaptor.getValue()).isBetween(beforeCall + delayMs - 1000, beforeCall + delayMs + 10000);
    }

    @Test
    @DisplayName("sendWithDelay should use correct queue name for dunning retry")
    void sendWithDelayShouldUseCorrectDunningQueue() {
        publisher.sendWithDelay("payu.billing.scheduled", "dunning-sub-99", 300000L);

        verify(jmsTemplate).convertAndSend(eq("payu.billing.scheduled"), eq("dunning-sub-99"), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("sendWithDelay object message should be passed through")
    void sendWithDelayShouldPassObjectMessage() {
        UUID subscriptionId = UUID.randomUUID();

        publisher.sendWithDelay("payu.billing.scheduled", subscriptionId.toString(), 300000L);

        verify(jmsTemplate).convertAndSend(eq("payu.billing.scheduled"), eq(subscriptionId.toString()), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("send should not set any AMQ headers")
    void sendShouldNotSetAmqHeaders() {
        publisher.send("payu.notification.commands", "notify-me");

        verify(jmsTemplate).convertAndSend(eq("payu.notification.commands"), eq("notify-me"));
        verify(jmsTemplate, never()).convertAndSend(anyString(), any(), any(MessagePostProcessor.class));
    }
}
