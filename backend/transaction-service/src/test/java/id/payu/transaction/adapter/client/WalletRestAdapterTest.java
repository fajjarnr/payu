package id.payu.transaction.adapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletRestAdapterTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void forwardsAuthorizationWhenCommittingReservation() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WalletRestAdapter adapter = new WalletRestAdapter(restTemplate, new ObjectMapper());
        setWalletServiceUrl(adapter, "http://wallet-service:8080");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer transfer-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        server.expect(requestTo("http://wallet-service:8080/api/v1/wallets/reservations/reservation-001/commit"))
                .andExpect(header("Authorization", "Bearer transfer-token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.commitBalance(UUID.randomUUID(), "transaction-001", "reservation-001", BigDecimal.TEN);

        server.verify();
    }

    private static void setWalletServiceUrl(WalletRestAdapter adapter, String url) throws Exception {
        Field field = WalletRestAdapter.class.getDeclaredField("walletServiceUrl");
        field.setAccessible(true);
        field.set(adapter, url);
    }
}
