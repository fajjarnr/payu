package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.Card;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CardUseCase {
    Card createVirtualCard(String accountId, String cardHolderName, BigDecimal dailyLimit);

    List<Card> getCardsByAccountId(String accountId);

    Optional<Card> getCardById(String cardId);

    void freezeCard(String cardId);

    void unfreezeCard(String cardId);
}
