package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PROD-045: SmsSender LOG-mode output (recipient + title + body) must be
 * free of raw PII. Plain JUnit — no Quarkus runtime needed.
 */
class SmsSenderLogSanitizationTest {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final Handler captureHandler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            buffer.writeBytes((new SimpleFormatter().format(record)).getBytes());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    private Logger attachLogCapture() {
        Logger logger = Logger.getLogger(SmsSender.class.getName());
        logger.addHandler(captureHandler);
        logger.setUseParentHandlers(false);
        return logger;
    }

    @AfterEach
    void detachLogCapture() {
        Logger.getLogger(SmsSender.class.getName()).removeHandler(captureHandler);
    }

    @Test
    @DisplayName("never contains raw recipient, title or body")
    void neverContainsRawPii() {
        attachLogCapture();
        SmsSender sender = new SmsSender();
        sender.smsProvider = "LOG";
        Notification notification = new Notification();
        notification.setRecipient("+6281234567890");
        notification.setTitle("Your OTP for transfer");
        notification.setBody("Dear user, your OTP is 123456");
        notification.setUserId("user-123");

        sender.send(notification);

        String logs = buffer.toString();
        assertThat(logs)
                .as("SMS LOG-mode output must not contain raw recipient, title or body")
                .doesNotContain("+6281234567890")
                .doesNotContain("Your OTP for transfer")
                .doesNotContain("your OTP is 123456");
        assertThat(logs)
                .as("masked recipient must still be present for ops traceability")
                .contains("+628****7890");
    }
}
