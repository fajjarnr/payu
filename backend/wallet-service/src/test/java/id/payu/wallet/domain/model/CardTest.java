package id.payu.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CardTest {

    @Test
    @DisplayName("Should create card with builder")
    void shouldCreateCardWithBuilder() {
        UUID walletId = UUID.randomUUID();
        Card card = Card.builder()
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

        assertThat(card).isNotNull();
        assertThat(card.getWalletId()).isEqualTo(walletId);
        assertThat(card.getCardNumber()).isEqualTo("4111222233334444");
        assertThat(card.getStatus()).isEqualTo(Card.CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return true when card is active")
    void shouldReturnTrueWhenCardIsActive() {
        Card card = Card.builder()
                .status(Card.CardStatus.ACTIVE)
                .build();

        assertThat(card.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should return false when card is not active")
    void shouldReturnFalseWhenCardIsNotActive() {
        Card card = Card.builder()
                .status(Card.CardStatus.FROZEN)
                .build();

        assertThat(card.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should freeze card")
    void shouldFreezeCard() {
        Card card = Card.builder()
                .status(Card.CardStatus.ACTIVE)
                .updatedAt(LocalDateTime.now())
                .build();

        card.freeze();

        assertThat(card.getStatus()).isEqualTo(Card.CardStatus.FROZEN);
    }

    @Test
    @DisplayName("Should unfreeze frozen card")
    void shouldUnfreezeFrozenCard() {
        Card card = Card.builder()
                .status(Card.CardStatus.FROZEN)
                .updatedAt(LocalDateTime.now())
                .build();

        card.unfreeze();

        assertThat(card.getStatus()).isEqualTo(Card.CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should not unfreeze non-frozen card")
    void shouldNotUnfreezeNonFrozenCard() {
        Card card = Card.builder()
                .status(Card.CardStatus.ACTIVE)
                .updatedAt(LocalDateTime.now())
                .build();

        card.unfreeze();

        assertThat(card.getStatus()).isEqualTo(Card.CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should create card with constructor")
    void shouldCreateCardWithConstructor() {
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Card card = new Card(
                id, walletId, "4111222233334444", "123", "12/30", "John Doe",
                Card.CardStatus.ACTIVE, new BigDecimal("10000000"), now, now
        );

        assertThat(card.getId()).isEqualTo(id);
        assertThat(card.getWalletId()).isEqualTo(walletId);
        assertThat(card.getCardNumber()).isEqualTo("4111222233334444");
    }
}
