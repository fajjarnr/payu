package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.wallet.domain.model.Card;
import id.payu.wallet.domain.port.in.CardUseCase;
import id.payu.wallet.dto.CardResponse;
import id.payu.wallet.dto.CreateCardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for card operations.
 * Driving adapter in Hexagonal Architecture.
 */
@RestController
@RequestMapping("/api/v1/cards")
@Tag(name = "Card", description = "Virtual card management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CardController extends BaseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CardController.class);

    private final CardUseCase cardUseCase;

    public CardController(CardUseCase cardUseCase) {
        this.cardUseCase = cardUseCase;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create virtual card", description = "Creates a new virtual card for an account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Card created successfully",
            content = @Content(schema = @Schema(implementation = CardResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<CardResponse>> createCard(@RequestBody CreateCardRequest request) {
        Card card = cardUseCase.createVirtualCard(
                request.accountId(),
                request.cardHolderName(),
                request.dailyLimit());
        return created(toCardResponse(card), "/api/v1/cards/" + card.getId());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get cards by account", description = "Retrieves all cards for a specific account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
            content = @Content(schema = @Schema(implementation = CardResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<List<CardResponse>>> getCards(
            @Parameter(description = "Account ID", required = true) @RequestParam String accountId) {
        List<Card> cards = cardUseCase.getCardsByAccountId(accountId);
        return ok(cards.stream()
                .map(this::toCardResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get card by ID", description = "Retrieves a specific card by its ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Card found",
            content = @Content(schema = @Schema(implementation = CardResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Card not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<CardResponse>> getCardById(
            @Parameter(description = "Card ID", required = true) @PathVariable String cardId) {
        return cardUseCase.getCardById(cardId)
                .map(card -> ok(toCardResponse(card)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("CARD_001", "Card not found")));
    }

    @PostMapping("/{cardId}/freeze")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Freeze card", description = "Freezes a card to prevent transactions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Card frozen successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Card not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<Void>> freezeCard(
            @Parameter(description = "Card ID", required = true) @PathVariable String cardId) {
        cardUseCase.freezeCard(cardId);
        return ok(null);
    }

    @PostMapping("/{cardId}/unfreeze")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unfreeze card", description = "Unfreezes a card to allow transactions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Card unfrozen successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Card not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<Void>> unfreezeCard(
            @Parameter(description = "Card ID", required = true) @PathVariable String cardId) {
        cardUseCase.unfreezeCard(cardId);
        return ok(null);
    }

    /**
     * Masks a card number to show only the last 4 digits.
     * e.g., "4111111111111111" → "************1111"
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() <= 4) {
            return cardNumber;
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }

    private CardResponse toCardResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .walletId(card.getWalletId())
                .cardNumber(maskCardNumber(card.getCardNumber()))
                .expiryDate(card.getExpiryDate())
                .cardHolderName(card.getCardHolderName())
                .status(card.getStatus().name())
                .dailyLimit(card.getDailyLimit())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
