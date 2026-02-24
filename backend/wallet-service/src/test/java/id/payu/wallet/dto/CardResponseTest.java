package id.payu.wallet.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CardResponse} focusing on PCI-DSS compliance
 * for card number masking in API responses.
 */
class CardResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should mask card number showing only last 4 digits in API response")
    void shouldMaskCardNumberInApiResponse() {
        // Given: Full 16-digit card number
        String fullCardNumber = "4111222233334444";
        CardResponse response = CardResponse.builder()
                .id(UUID.randomUUID())
                .walletId(UUID.randomUUID())
                .cardNumber(fullCardNumber)
                .expiryDate("12/30")
                .cardHolderName("John Doe")
                .status("ACTIVE")
                .dailyLimit(new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .build();

        // When: getCardNumber() is called (used in API response via @JsonProperty)
        String maskedNumber = response.getCardNumber();

        // Then: Should return masked format with only last 4 digits visible
        assertThat(maskedNumber).isEqualTo("**** **** **** 4444");
        // And: Full card number should NOT be exposed
        assertThat(maskedNumber).doesNotContain("4111", "2222", "3333");
    }

    @Test
    @DisplayName("Should provide full card number via internal accessor only")
    void shouldProvideFullCardNumberViaInternalAccessor() {
        // Given: Full 16-digit card number
        String fullCardNumber = "4111222233334444";
        CardResponse response = CardResponse.builder()
                .id(UUID.randomUUID())
                .walletId(UUID.randomUUID())
                .cardNumber(fullCardNumber)
                .expiryDate("12/30")
                .cardHolderName("John Doe")
                .status("ACTIVE")
                .dailyLimit(new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .build();

        // When: getFullCardNumber() is called (internal use only, @JsonIgnore)
        String fullNumber = response.getFullCardNumber();

        // Then: Should return the full card number for internal processing
        assertThat(fullNumber).isEqualTo(fullCardNumber);
    }

    @Test
    @DisplayName("Should return default mask for null card number")
    void shouldReturnDefaultMaskForNullCardNumber() {
        // Given: CardResponse with null card number
        CardResponse response = CardResponse.builder()
                .id(UUID.randomUUID())
                .walletId(UUID.randomUUID())
                .cardNumber(null)
                .expiryDate("12/30")
                .cardHolderName("John Doe")
                .status("ACTIVE")
                .dailyLimit(new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .build();

        // When: getCardNumber() is called
        String maskedNumber = response.getCardNumber();

        // Then: Should return full mask
        assertThat(maskedNumber).isEqualTo("**** **** **** ****");
    }

    @Test
    @DisplayName("Should return default mask for short card number")
    void shouldReturnDefaultMaskForShortCardNumber() {
        // Given: CardResponse with short card number (less than 4 digits)
        CardResponse response = CardResponse.builder()
                .id(UUID.randomUUID())
                .walletId(UUID.randomUUID())
                .cardNumber("123")
                .expiryDate("12/30")
                .cardHolderName("John Doe")
                .status("ACTIVE")
                .dailyLimit(new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .build();

        // When: getCardNumber() is called
        String maskedNumber = response.getCardNumber();

        // Then: Should return full mask
        assertThat(maskedNumber).isEqualTo("**** **** **** ****");
    }

    @Test
    @DisplayName("Should serialize card number as masked in JSON response")
    void shouldSerializeCardNumberAsMaskedInJson() throws JsonProcessingException {
        // Given: CardResponse with full card number
        String fullCardNumber = "5555666677778888";
        CardResponse response = CardResponse.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .walletId(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"))
                .cardNumber(fullCardNumber)
                .expiryDate("12/30")
                .cardHolderName("Jane Doe")
                .status("ACTIVE")
                .dailyLimit(new BigDecimal("5000000"))
                .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .build();

        // When: Serialized to JSON
        String json = objectMapper.writeValueAsString(response);

        // Then: JSON should contain masked card number
        assertThat(json).contains("\"cardNumber\":\"**** **** **** 8888\"");
        // And: JSON should NOT contain full card number
        assertThat(json).doesNotContain("5555", "6666", "7777");
    }

    @Test
    @DisplayName("Should handle 15-digit AMEX card number masking")
    void shouldHandleAmexCardNumberMasking() {
        // Given: 15-digit AMEX card number
        String amexCardNumber = "378282246310005";
        CardResponse response = CardResponse.builder()
                .id(UUID.randomUUID())
                .walletId(UUID.randomUUID())
                .cardNumber(amexCardNumber)
                .expiryDate("12/30")
                .cardHolderName("John Doe")
                .status("ACTIVE")
                .dailyLimit(new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .build();

        // When: getCardNumber() is called
        String maskedNumber = response.getCardNumber();

        // Then: Should return masked format with only last 4 digits visible
        assertThat(maskedNumber).isEqualTo("**** **** **** 0005");
    }

    @Test
    @DisplayName("PCI-DSS compliance: Full card number must never appear in API response")
    void pciDssCompliance_FullCardNumberMustNeverAppearInApiResponse() throws JsonProcessingException {
        // Given: CardResponse with sensitive card number
        String sensitiveCardNumber = "4532015112830366";
        CardResponse response = CardResponse.builder()
                .id(UUID.randomUUID())
                .walletId(UUID.randomUUID())
                .cardNumber(sensitiveCardNumber)
                .expiryDate("12/30")
                .cardHolderName("John Doe")
                .status("ACTIVE")
                .dailyLimit(new BigDecimal("10000000"))
                .createdAt(LocalDateTime.now())
                .build();

        // When: Serialized to JSON (simulating API response)
        String json = objectMapper.writeValueAsString(response);

        // Then: Full card number must NOT appear anywhere in the JSON
        assertThat(json).doesNotContain(sensitiveCardNumber);
        assertThat(json).doesNotContain("453201", "511283");
        // And: Only last 4 digits should be visible
        assertThat(json).contains("0366");
    }
}
