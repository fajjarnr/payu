package id.payu.simulator.bifast.interfaces.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransferRequestTest {

    @Test
    void acceptsTransactionServiceTransferFieldNames() throws Exception {
        TransferRequest request = new ObjectMapper().readValue("""
                {
                  "referenceNumber": "disbursement-001",
                  "senderAccountNumber": "PAYU00000000",
                  "senderAccountName": "PayU Disbursement",
                  "beneficiaryBankCode": "BCA",
                  "beneficiaryAccountNumber": "1234567890",
                  "amount": 10.00,
                  "currency": "IDR",
                  "webhookUrl": "http://transaction-service:8080/api/v1/disbursements/callback"
                }
                """, TransferRequest.class);

        assertEquals("PAYU", request.sourceBankCode());
        assertEquals("PAYU00000000", request.sourceAccountNumber());
        assertEquals("PayU Disbursement", request.sourceAccountName());
        assertEquals("BCA", request.destinationBankCode());
        assertEquals("1234567890", request.destinationAccountNumber());
    }
}
