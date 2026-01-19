package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.Card;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for card persistence operations.
 */
public interface CardPersistencePort {
    
    Card save(Card card);
    
    Optional<Card> findById(UUID id);
    
    List<Card> findByWalletId(UUID walletId);
    
    boolean existsByCardNumber(String cardNumber);
}
