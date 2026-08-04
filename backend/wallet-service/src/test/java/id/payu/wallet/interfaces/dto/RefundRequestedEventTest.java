package id.payu.wallet.interfaces.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundRequestedEventTest {

    @Test
    void deserializesCloudEventDataWithLedgerOperation() throws Exception {
        String payload = "{\"amount\":\"100.00\",\"currency\":\"IDR\","
                + "\"reason\":\"customer refund\",\"refundId\":\"550e8400-e29b-41d4-a716-446655440001\","
                + "\"transactionId\":\"550e8400-e29b-41d4-a716-446655440002\","
                + "\"ledgerOperation\":\"REVERSAL\",\"senderAccountId\":\"sender\","
                + "\"recipientAccountId\":\"recipient\"}";

        RefundRequestedEvent event = new ObjectMapper().readValue(payload, RefundRequestedEvent.class);

        assertThat(event.recipientAccountId()).isEqualTo("recipient");
    }
}
