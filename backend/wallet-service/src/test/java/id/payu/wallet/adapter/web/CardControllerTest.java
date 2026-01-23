package id.payu.wallet.adapter.web;

import id.payu.wallet.domain.model.Card;
import id.payu.wallet.domain.port.in.CardUseCase;
import id.payu.wallet.dto.CardResponse;
import id.payu.wallet.dto.CreateCardRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    @Mock
    private CardUseCase cardUseCase;

    @InjectMocks
    private CardController cardController;

    private Card testCard;

    @BeforeEach
    void setUp() {
        UUID walletId = UUID.randomUUID();
        testCard = Card.builder()
                .id(UUID.randomUUID())
                .walletId(walletId)
                .cardNumber("4111222233334444")
                .cvv("123")
                .expiryDate("12/30")
                .cardHolderName("John Doe")
                .status(Card.CardStatus.ACTIVE)
                .dailyLimit(new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create virtual card")
    void shouldCreateVirtualCard() {
        CreateCardRequest request = new CreateCardRequest("ACC-001", "John Doe", new BigDecimal("5000000"));

        when(cardUseCase.createVirtualCard("ACC-001", "John Doe", new BigDecimal("5000000")))
                .thenReturn(testCard);

        ResponseEntity<CardResponse> response = cardController.createCard(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCardNumber()).isEqualTo(testCard.getCardNumber());
        assertThat(response.getBody().getStatus()).isEqualTo(Card.CardStatus.ACTIVE.name());
        verify(cardUseCase).createVirtualCard("ACC-001", "John Doe", new BigDecimal("5000000"));
    }

    @Test
    @DisplayName("Should get cards by account ID")
    void shouldGetCardsByAccountId() {
        when(cardUseCase.getCardsByAccountId("ACC-001")).thenReturn(List.of(testCard));

        ResponseEntity<List<CardResponse>> response = cardController.getCards("ACC-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCardNumber()).isEqualTo(testCard.getCardNumber());
        verify(cardUseCase).getCardsByAccountId("ACC-001");
    }

    @Test
    @DisplayName("Should freeze card")
    void shouldFreezeCard() {
        String cardId = testCard.getId().toString();

        ResponseEntity<Void> response = cardController.freezeCard(cardId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cardUseCase).freezeCard(cardId);
    }

    @Test
    @DisplayName("Should unfreeze card")
    void shouldUnfreezeCard() {
        String cardId = testCard.getId().toString();

        ResponseEntity<Void> response = cardController.unfreezeCard(cardId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cardUseCase).unfreezeCard(cardId);
    }

    @Test
    @DisplayName("Should get card by ID")
    void shouldGetCardById() {
        String cardId = testCard.getId().toString();
        when(cardUseCase.getCardById(cardId)).thenReturn(Optional.of(testCard));

        ResponseEntity<CardResponse> response = cardController.getCardById(cardId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCardNumber()).isEqualTo(testCard.getCardNumber());
        verify(cardUseCase).getCardById(cardId);
    }

    @Test
    @DisplayName("Should return 404 when card not found")
    void shouldReturn404WhenCardNotFound() {
        String cardId = UUID.randomUUID().toString();
        when(cardUseCase.getCardById(cardId)).thenReturn(Optional.empty());

        ResponseEntity<CardResponse> response = cardController.getCardById(cardId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
