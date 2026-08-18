package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.Card;
import id.payu.wallet.domain.model.CardStatus;
import id.payu.wallet.domain.port.out.CardPersistencePort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CardServiceTest {

    private CardPersistencePort cardPersistencePort;
    private WalletPersistencePort walletPersistencePort;
    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardPersistencePort = mock(CardPersistencePort.class);
        walletPersistencePort = mock(WalletPersistencePort.class);
        cardService = new CardService(cardPersistencePort, walletPersistencePort);
    }

    private Card card(String cardId, CardStatus status) {
        return Card.builder()
                .id(UUID.fromString(cardId))
                .walletId(UUID.randomUUID())
                .cardNumber("4111111111111111")
                .expiryDate("01/32")
                .cardHolderName("Test User")
                .status(status)
                .dailyLimit(new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void updateCardLimitUpdatesDailyLimit() {
        Card card = card("00000000-0000-0000-0000-000000000001", CardStatus.ACTIVE);
        when(cardPersistencePort.findById(card.getId())).thenReturn(Optional.of(card));
        when(cardPersistencePort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card updated = cardService.updateCardLimit(card.getId().toString(), new BigDecimal("5000000"));

        assertThat(updated.getDailyLimit()).isEqualByComparingTo("5000000");
        verify(cardPersistencePort).save(card);
    }

    @Test
    void updateCardLimitRejectsNegativeLimit() {
        Card card = card("00000000-0000-0000-0000-000000000001", CardStatus.ACTIVE);
        when(cardPersistencePort.findById(card.getId())).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.updateCardLimit(card.getId().toString(), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void closeCardTransitionsToCancelled() {
        Card card = card("00000000-0000-0000-0000-000000000001", CardStatus.ACTIVE);
        when(cardPersistencePort.findById(card.getId())).thenReturn(Optional.of(card));
        when(cardPersistencePort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card closed = cardService.closeCard(card.getId().toString());

        assertThat(closed.getStatus()).isEqualTo(CardStatus.CANCELLED);
        verify(cardPersistencePort).save(card);
    }
}
