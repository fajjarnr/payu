package id.payu.wallet.adapter.web;

import id.payu.wallet.domain.model.Card;
import id.payu.wallet.domain.port.in.CardUseCase;
import id.payu.wallet.dto.CardResponse;
import id.payu.wallet.dto.CreateCardRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardUseCase cardUseCase;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@RequestBody CreateCardRequest request) {
        Card card = cardUseCase.createVirtualCard(
                request.accountId(),
                request.cardHolderName(),
                request.dailyLimit());
        return ResponseEntity.created(URI.create("/api/v1/cards/" + card.getId()))
                .body(CardResponse.from(card));
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getCards(@RequestParam String accountId) {
        List<Card> cards = cardUseCase.getCardsByAccountId(accountId);
        return ResponseEntity.ok(cards.stream()
                .map(CardResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable String cardId) {
        return cardUseCase.getCardById(cardId)
                .map(card -> ResponseEntity.ok(CardResponse.from(card)))
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
}
