package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.Card;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.port.out.CardPersistencePort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardPersistencePort cardPersistencePort;

    @Mock
    private WalletPersistencePort walletPersistencePort;

    @InjectMocks
    private CardService cardService;

    private Wallet testWallet;
    private Card testCard;

    @BeforeEach
    void setUp() {
        testWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .balance(BigDecimal.ZERO)
                .build();

        testCard = Card.builder()
                .id(UUID.randomUUID())
                .walletId(testWallet.getId())
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
    @DisplayName("Should create virtual card successfully")
    void shouldCreateVirtualCard() {
        // Given
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));
        when(cardPersistencePort.existsByCardNumber(anyString())).thenReturn(false);
        when(cardPersistencePort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Card createdCard = cardService.createVirtualCard("ACC-001", "John Doe", new BigDecimal("5000000"));

        // Then
        assertThat(createdCard).isNotNull();
        assertThat(createdCard.getWalletId()).isEqualTo(testWallet.getId());
        assertThat(createdCard.getCardHolderName()).isEqualTo("John Doe");
        assertThat(createdCard.getDailyLimit()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(createdCard.getCardNumber()).startsWith("4"); // Visa
        assertThat(createdCard.getStatus()).isEqualTo(Card.CardStatus.ACTIVE);

        verify(cardPersistencePort).save(any(Card.class));
    }

    @Test
    @DisplayName("Should get cards by account ID")
    void shouldGetCardsByAccountId() {
        // Given
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));
        when(cardPersistencePort.findByWalletId(testWallet.getId())).thenReturn(List.of(testCard));

        // When
        List<Card> cards = cardService.getCardsByAccountId("ACC-001");

        // Then
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getId()).isEqualTo(testCard.getId());
    }

    @Test
    @DisplayName("Should freeze card")
    void shouldFreezeCard() {
        // Given
        UUID cardId = testCard.getId();
        when(cardPersistencePort.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardPersistencePort.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        cardService.freezeCard(cardId.toString());

        // Then
        assertThat(testCard.getStatus()).isEqualTo(Card.CardStatus.FROZEN);
        verify(cardPersistencePort).save(testCard);
    }
}
