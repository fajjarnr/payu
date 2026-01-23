package id.payu.wallet.exception;

import id.payu.wallet.application.exception.WalletNotFoundException;
import id.payu.wallet.application.service.InsufficientBalanceException;
import id.payu.wallet.application.service.ReservationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle wallet not found exception")
    void shouldHandleWalletNotFoundException() {
        WalletNotFoundException ex = new WalletNotFoundException("ACC-001");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleWalletNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should handle insufficient balance exception")
    void shouldHandleInsufficientBalanceException() {
        InsufficientBalanceException ex = new InsufficientBalanceException("ACC-001", new BigDecimal("10000"), new BigDecimal("5000"));

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInsufficientBalance(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should handle reservation not found exception")
    void shouldHandleReservationNotFoundException() {
        ReservationNotFoundException ex = new ReservationNotFoundException("RES-001");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleReservationNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should handle generic exception")
    void shouldHandleGenericException() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
