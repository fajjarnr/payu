package id.payu.transaction.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation tests for {@link InitiateTransferRequest} DTO.
 *
 * Tests verify that the enhanced validation constraints prevent:
 * - SQL injection via account number pattern restrictions
 * - DoS attacks via size constraints
 * - Invalid amounts via precision validation
 * - Injection attacks via description pattern restrictions
 *
 * PCI-DSS Compliance:
 * - Requirement 6.5.1: Injection flaws (input validation)
 * - OWASP: Input validation for financial transactions
 */
class InitiateTransferRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should validate valid transfer request")
    void shouldValidateValidTransferRequest() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .currency("IDR")
                .description("Transfer to John")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .transactionPin("123456")
                .deviceId("device-123")
                .idempotencyKey("idemp-123")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Sender Account ID tests

    @Test
    @DisplayName("Should reject null sender account ID")
    void shouldRejectNullSenderAccountId() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(null)
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("senderAccountId"));
    }

    // Recipient Account Number tests

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should reject null or empty recipient account number")
    void shouldRejectNullOrEmptyRecipientAccountNumber(String accountNumber) {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber(accountNumber)
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("recipientAccountNumber"));
    }

    @Test
    @DisplayName("Should reject account number shorter than 10 digits")
    void shouldRejectShortAccountNumber() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456789")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("between 10 and 20"));
    }

    @Test
    @DisplayName("Should reject account number longer than 20 digits")
    void shouldRejectLongAccountNumber() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456789012345678901")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("between 10 and 20"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123-456-7890", "123 456 7890", "ABC1234567", "123456789!"}) // pragma: allowlist secret
    @DisplayName("Should reject account number with non-digit characters")
    void shouldRejectAccountNumberWithNonDigits(String accountNumber) {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber(accountNumber)
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("only digits"));
    }

    @Test
    @DisplayName("Should reject account number with SQL injection pattern")
    void shouldRejectSQLInjectionInAccountNumber() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123' OR '1'='1")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("recipientAccountNumber"));
    }

    // Amount validation tests

    @Test
    @DisplayName("Should reject null amount")
    void shouldRejectNullAmount() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(null)
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("Should reject zero amount")
    void shouldRejectZeroAmount() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(BigDecimal.ZERO)
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("Should reject negative amount")
    void shouldRejectNegativeAmount() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("-100"))
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty();
    }

    @Test
    @DisplayName("Should accept amount with 2 decimal places")
    void shouldAcceptAmountWithTwoDecimals() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100.50"))
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    // Currency validation tests

    @ParameterizedTest
    @ValueSource(strings = {"IDR", "USD", "EUR", "SGD", "JPY"})
    @DisplayName("Should accept valid ISO 4217 currency codes")
    void shouldAcceptValidCurrencyCodes(String currency) {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .currency(currency)
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "invalid", "ID", "INDONESIA"})
    @DisplayName("Should reject invalid currency codes")
    void shouldRejectInvalidCurrencyCodes(String currency) {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .currency(currency)
                .description("Test transfer")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("ISO 4217"));
    }

    // Description validation tests

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should reject null or empty description")
    void shouldRejectNullOrEmptyDescription(String description) {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description(description)
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    @DisplayName("Should reject description longer than 100 characters")
    void shouldRejectLongDescription() {
        // Given
        String longDescription = "a".repeat(101);
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description(longDescription)
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("between 1 and 100"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<script>alert('xss')</script>", "Transfer'; DROP TABLE users--", "description with @#$% symbols"})
    @DisplayName("Should reject description with invalid characters")
    void shouldRejectDescriptionWithInvalidCharacters(String description) {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description(description)
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    @DisplayName("Should accept description with valid special characters")
    void shouldAcceptDescriptionWithValidSpecialCharacters() {
        // Given
        String[] validDescriptions = {
                "Transfer to John Doe",
                "Payment-Invoice #123",
                "Monthly rental, Q3 2024",
                "Bill payment (electricity)"
        };

        for (String description : validDescriptions) {
            InitiateTransferRequest request = InitiateTransferRequest.builder()
                    .senderAccountId(UUID.randomUUID())
                    .recipientAccountNumber("1234567890")
                    .amount(new BigDecimal("100000"))
                    .description(description)
                    .build();

            // When
            Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations)
                    .as("Description '%s' should be valid", description)
                    .noneMatch(v -> v.getPropertyPath().toString().equals("description"));
        }
    }

    // Transaction PIN validation tests

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "abcdef", "12345!", "12 34 56"})
    @DisplayName("Should reject invalid transaction PIN formats")
    void shouldRejectInvalidTransactionPin(String pin) {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .transactionPin(pin)
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("transactionPin"));
    }

    @Test
    @DisplayName("Should accept valid 6-digit transaction PIN")
    void shouldAcceptValidTransactionPin() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .transactionPin("123456")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("transactionPin"));
    }

    // Device ID validation tests

    @Test
    @DisplayName("Should reject device ID longer than 100 characters")
    void shouldRejectLongDeviceId() {
        // Given
        String longDeviceId = "a".repeat(101);
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .deviceId(longDeviceId)
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("deviceId"));
    }

    // Idempotency Key validation tests

    @Test
    @DisplayName("Should accept valid idempotency key")
    void shouldAcceptValidIdempotencyKey() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .idempotencyKey("idemp-123-abc")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("idempotencyKey"));
    }

    @Test
    @DisplayName("Should reject idempotency key with invalid characters")
    void shouldRejectInvalidIdempotencyKey() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .idempotencyKey("idemp@#$%123")
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("idempotencyKey"));
    }

    @Test
    @DisplayName("Should reject idempotency key longer than 100 characters")
    void shouldRejectLongIdempotencyKey() {
        // Given
        String longIdempotencyKey = "a".repeat(101);
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .description("Test transfer")
                .idempotencyKey(longIdempotencyKey)
                .build();

        // When
        Set<ConstraintViolation<InitiateTransferRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("idempotencyKey"));
    }
}
