package id.payu.wallet.adapter.web;

import id.payu.wallet.application.service.WalletService;
import id.payu.wallet.config.SecurityConfig;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.dto.CreditRequest;
import id.payu.wallet.dto.ReserveBalanceRequest;
import id.payu.wallet.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("WalletController Tests")
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletUseCase walletUseCase;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .balance(new BigDecimal("1000000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .currency("IDR")
                .status(Wallet.WalletStatus.ACTIVE)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{accountId}/balance - should return balance")
    void shouldReturnBalance() throws Exception {
        // Given
        when(walletUseCase.getWalletByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));

        // When/Then
        mockMvc.perform(get("/api/v1/wallets/ACC-001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-001"))
                .andExpect(jsonPath("$.balance").value(1000000.00))
                .andExpect(jsonPath("$.availableBalance").value(1000000.00))
                .andExpect(jsonPath("$.currency").value("IDR"));
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{accountId}/balance - should return 404 when not found")
    void shouldReturn404WhenWalletNotFound() throws Exception {
        // Given
        when(walletUseCase.getWalletByAccountId("UNKNOWN")).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/v1/wallets/UNKNOWN/balance"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/wallets/{accountId}/reserve - should reserve balance")
    void shouldReserveBalance() throws Exception {
        // Given
        ReserveBalanceRequest request = ReserveBalanceRequest.builder()
                .amount(new BigDecimal("100000.00"))
                .referenceId("TXN-001")
                .build();

        when(walletUseCase.reserveBalance(eq("ACC-001"), any(), eq("TXN-001")))
                .thenReturn("reservation-uuid-123");

        // When/Then
        mockMvc.perform(post("/api/v1/wallets/ACC-001/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value("reservation-uuid-123"))
                .andExpect(jsonPath("$.accountId").value("ACC-001"))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    @DisplayName("POST /api/v1/wallets/{accountId}/reserve - should validate request")
    void shouldValidateReserveRequest() throws Exception {
        // Given - Invalid request (missing amount)
        ReserveBalanceRequest request = ReserveBalanceRequest.builder()
                .referenceId("TXN-001")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/wallets/ACC-001/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @DisplayName("POST /api/v1/wallets/{accountId}/credit - should credit amount")
    void shouldCreditAmount() throws Exception {
        // Given
        CreditRequest request = CreditRequest.builder()
                .amount(new BigDecimal("50000.00"))
                .referenceId("TXN-002")
                .description("Incoming transfer")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/wallets/ACC-001/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREDITED"))
                .andExpect(jsonPath("$.accountId").value("ACC-001"));
    }

    @Test
    @DisplayName("POST /api/v1/wallets/reservations/{id}/commit - should commit reservation")
    void shouldCommitReservation() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/wallets/reservations/res-123/commit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMMITTED"))
                .andExpect(jsonPath("$.reservationId").value("res-123"));
    }
}
