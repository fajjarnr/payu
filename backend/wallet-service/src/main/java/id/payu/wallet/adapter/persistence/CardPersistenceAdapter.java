package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.CardEntity;
import id.payu.wallet.adapter.persistence.repository.CardJpaRepository;
import id.payu.wallet.domain.model.Card;
import id.payu.wallet.domain.port.out.CardPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CardPersistenceAdapter implements CardPersistencePort {

    private final CardJpaRepository cardJpaRepository;

    @Override
    public Card save(Card card) {
        CardEntity entity;
        if (card.getId() != null && cardJpaRepository.existsById(card.getId())) {
            entity = cardJpaRepository.findById(card.getId()).orElseThrow();
            entity.setWalletId(card.getWalletId());
            entity.setCardNumber(card.getCardNumber());
            entity.setExpiryDate(card.getExpiryDate());
            entity.setCardHolderName(card.getCardHolderName());
            entity.setStatus(card.getStatus());
            entity.setDailyLimit(card.getDailyLimit());
            entity.setUpdatedAt(card.getUpdatedAt());
        } else {
            entity = CardEntity.fromDomain(card);
        }
        CardEntity saved = cardJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Card> findById(UUID id) {
        return cardJpaRepository.findById(id)
                .map(CardEntity::toDomain);
    }

    @Override
    public List<Card> findByWalletId(UUID walletId) {
        return cardJpaRepository.findByWalletId(walletId).stream()
                .map(CardEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCardNumber(String cardNumber) {
        return cardJpaRepository.existsByCardNumber(cardNumber);
    }
}
