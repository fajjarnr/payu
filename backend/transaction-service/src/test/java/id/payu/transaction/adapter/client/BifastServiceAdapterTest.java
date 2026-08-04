package id.payu.transaction.adapter.client;

import id.payu.transaction.dto.BifastTransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BifastServiceAdapterTest {

    @Test
    void postsTransferToSimulatorResourcePath() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BifastServiceAdapter adapter = new BifastServiceAdapter(restTemplate);
        setField(adapter, "bifastServiceUrl", "http://bi-fast-simulator:8080");

        BifastTransferRequest request = BifastTransferRequest.builder()
                .referenceNumber("disbursement-001")
                .beneficiaryBankCode("BCA")
                .beneficiaryAccountNumber("1234567890")
                .beneficiaryAccountName("JOHN DOE")
                .amount(BigDecimal.TEN)
                .currency("IDR")
                .senderAccountNumber("PAYU00000000")
                .senderAccountName("PayU Disbursement")
                .purposeCode("PAY")
                .build();

        server.expect(requestTo("http://bi-fast-simulator:8080/api/v1/transfer"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.initiateTransfer(request);

        server.verify();
    }

    private static void setField(Object target, String name, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
