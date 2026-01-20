package id.payu.wallet.adapter.web;

import id.payu.wallet.domain.model.Card;
import id.payu.wallet.domain.port.in.CardUseCase;
import id.payu.wallet.dto.CardResponse;
import id.payu.wallet.dto.CreateCardRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CardController.class);

    private final CardUseCase cardUseCase;

    public CardController(CardUseCase cardUseCase) {
        this.cardUseCase = cardUseCase;
    }

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@RequestBody CreateCardRequest request) {
        Card card = cardUseCase.createVirtualCard(
                request.accountId(),
                request.cardHolderName(),
                request.dailyLimit());
        return ResponseEntity.created(URI.create("/api/v1/cards/" + card.getId()))
                .body(toCardResponse(card));
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getCards(@RequestParam String accountId) {
        List<Card> cards = cardUseCase.getCardsByAccountId(accountId);
        return ResponseEntity.ok(cards.stream()
                .map(this::toCardResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable String cardId) {
        return cardUseCase.getCardById(cardId)
                .map(card -> ResponseEntity.ok(toCardResponse(card)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{cardId}/freeze")
    public ResponseEntity<Void> freezeCard(@PathVariable String cardId) {
        cardUseCase.freezeCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/unfreeze")
    public ResponseEntity<Void> unfreezeCard(@PathVariable String cardId) {
        cardUseCase.unfreezeCard(cardId);
        return ResponseEntity.ok().build();
    }

    private CardResponse toCardResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .walletId(card.getWalletId())
                .cardNumber(card.getCardNumber())
                .expiryDate(card.getExpiryDate())
                .cardHolderName(card.getCardHolderName())
                .status(card.getStatus().name())
                .dailyLimit(card.getDailyLimit())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
