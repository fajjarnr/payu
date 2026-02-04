package id.payu.transaction.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class, excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionUseCase transactionUseCase;

    @Test
    @DisplayName("POST /api/v1/transactions/transfer - Success")
    void initiateTransferSuccess() throws Exception {
        // Given
        InitiateTransferRequest request = new InitiateTransferRequest();
        request.setSenderAccountId(UUID.randomUUID());
        request.setRecipientAccountNumber("1234567890");
        request.setAmount(new BigDecimal("100000"));
        request.setCurrency("IDR");
        request.setType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER);
        request.setDescription("Test");

        InitiateTransferCommandResult result = new InitiateTransferCommandResult(
                UUID.randomUUID(),
                "REF123",
                "PENDING",
                BigDecimal.ZERO,
                "2 seconds"
        );

        given(transactionUseCase.initiateTransfer(any(InitiateTransferRequest.class), anyString())).willReturn(result);

        // When/Then
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .with(csrf())
                        .with(jwt()
                                .jwt(j -> j.subject("user-123"))
                                .authorities(new SimpleGrantedAuthority("write:transaction")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.referenceNumber").value("REF123"));
    }
}
