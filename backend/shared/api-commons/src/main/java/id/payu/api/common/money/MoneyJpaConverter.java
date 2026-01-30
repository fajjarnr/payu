package id.payu.api.common.money;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * JPA AttributeConverter for {@link Money} objects.
 * <p>
 * Converts Money to database columns and back. Stores amount and currency code
 * as separate columns for efficient querying and indexing.
 *
 * <p>Database schema requirements:
 * <ul>
 *   <li>amount column: DECIMAL(19,4) or NUMERIC(19,4)</li>
 *   <li>currency_code column: VARCHAR(3)</li>
 * </ul>
 *
 * <p>Note: This converter is set to autoApply=false because Money is an embeddable
 * entity with its own column mappings. Use @Embedded for proper storage.
 * This converter is provided for cases where Money needs to be stored in a single column
 * as a composite value.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class MoneyJpaConverter implements AttributeConverter<Money, String> {

    private static final String DELIMITER = "|";

    /**
     * Converts Money to a String representation for database storage.
     * Format: "currencyCode|amount" (e.g., "IDR|100.50")
     *
     * @param money the Money to convert
     * @return string representation or null if money is null
     */
    @Override
    public String convertToDatabaseColumn(Money money) {
        if (money == null) {
            return null;
        }
        return money.getCurrencyCode() + DELIMITER + money.getAmount().toPlainString();
    }

    /**
     * Converts a String from the database to a Money object.
     *
     * @param dbData the database string value
     * @return Money instance or null if dbData is null
     * @throws IllegalArgumentException if the format is invalid
     */
    @Override
    public Money convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        String[] parts = dbData.split("\\|");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Money format in database: " + dbData);
        }

        String currencyCode = parts[0];
        BigDecimal amount = new BigDecimal(parts[1]);

        return Money.of(amount, currencyCode);
    }
}
