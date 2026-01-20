package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.Card;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.port.in.CardUseCase;
import id.payu.wallet.domain.port.out.CardPersistencePort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class CardService implements CardUseCase {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CardService.class);

    private final CardPersistencePort cardPersistencePort;
    private final WalletPersistencePort walletPersistencePort;
    private final Random random = new Random();

    public CardService(CardPersistencePort cardPersistencePort, 
                       WalletPersistencePort walletPersistencePort) {
        this.cardPersistencePort = cardPersistencePort;
        this.walletPersistencePort = walletPersistencePort;
    }

    @Override
    @Transactional
    public Card createVirtualCard(String accountId, String cardHolderName, BigDecimal dailyLimit) {
        log.info("Creating virtual card for account: {}", accountId);

        Wallet wallet = walletPersistencePort.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for account: " + accountId));

        String cardNumber;
        do {
            cardNumber = generateCardNumber();
        } while (cardPersistencePort.existsByCardNumber(cardNumber));

        String expiry = LocalDateTime.now().plusYears(5).format(DateTimeFormatter.ofPattern("MM/yy"));
        String cvv = String.format("%03d", random.nextInt(1000));

        Card card = Card.builder()
                .id(UUID.randomUUID())
                .walletId(wallet.getId())
                .cardNumber(cardNumber)
                .cvv(cvv)
                .expiryDate(expiry)
                .cardHolderName(cardHolderName)
                .status(Card.CardStatus.ACTIVE)
                .dailyLimit(dailyLimit != null ? dailyLimit : new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Card savedCard = cardPersistencePort.save(card);
        log.info("Virtual card created successfully: {}", savedCard.getId());
        return savedCard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> getCardsByAccountId(String accountId) {
        Wallet wallet = walletPersistencePort.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for account: " + accountId));

        return cardPersistencePort.findByWalletId(wallet.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> getCardById(String cardId) {
        return cardPersistencePort.findById(UUID.fromString(cardId));
    }

    @Override
    @Transactional
    public void freezeCard(String cardId) {
        Card card = cardPersistencePort.findById(UUID.fromString(cardId))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        card.freeze();
        cardPersistencePort.save(card);
        log.info("Card frozen: {}", cardId);
    }

    @Override
    @Transactional
    public void unfreezeCard(String cardId) {
        Card card = cardPersistencePort.findById(UUID.fromString(cardId))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        card.unfreeze();
        cardPersistencePort.save(card);
        log.info("Card unfrozen: {}", cardId);
    }

    // Simple mock generator (starts with 4 for Visa simulation)
    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder("4"); // Visa
        for (int i = 0; i < 15; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
