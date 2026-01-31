package id.payu.transaction.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionUseCase transactionUseCase;

    @Test
    @DisplayName("POST /v1/transactions/transfer - Success")
    @WithMockUser
    void initiateTransferSuccess() throws Exception {
        // Given
        InitiateTransferRequest request = new InitiateTransferRequest();
        request.setSenderAccountId(UUID.randomUUID());
        request.setRecipientAccountNumber("12345");
        request.setAmount(new BigDecimal("100000"));
        request.setCurrency("IDR");
        request.setType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER);
        request.setDescription("Test");

        InitiateTransferResponse response = InitiateTransferResponse.builder()
                .transactionId(UUID.randomUUID())
                .status("PENDING")
                .referenceNumber("REF123")
                .build();

        given(transactionUseCase.initiateTransfer(any())).willReturn(response);

        // When/Then
        mockMvc.perform(post("/v1/transactions/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.referenceNumber").value("REF123"));
    }
}
