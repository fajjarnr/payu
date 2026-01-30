package id.payu.security.converter;

import id.payu.security.crypto.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * JPA AttributeConverter for field-level encryption of sensitive data.
 *
 * <p>This converter automatically encrypts data when writing to the database
 * and decrypts it when reading from the database using AES-GCM encryption.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * @Convert(converter = EncryptedStringConverter.class)
 * @Column(name = "card_number")
 * private String cardNumber;
 * }</pre>
 *
 * <p><b>Security Notes:</b></p>
 * <ul>
 *   <li>Data is encrypted at rest using AES-GCM (256-bit key)</li>
 *   <li>Each encryption uses a unique IV (Initialization Vector)</li>
 *   <li>The encryption key must be configured via {@code payu.security.encryption-key}</li>
 *   <li>This converter requires the EncryptionService bean to be available</li>
 * </ul>
 *
 * @see EncryptionService
 * @see <a href="https://pcisecuritystandards.org/">PCI-DSS Requirements</a>
 */
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    /**
     * Spring automatically injects the EncryptionService bean.
     * We use a static field because JPA instantiates converters without dependency injection.
     */
    public static void setEncryptionService(EncryptionService service) {
        EncryptedStringConverter.encryptionService = service;
    }

    /**
     * Converts the attribute value to the database column representation.
     * Encrypts the value before storing it in the database.
     *
     * @param attribute the entity attribute value (plaintext)
     * @return the encrypted value for database storage
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized. " +
                    "Ensure security-starter is properly configured.");
        }

        return encryptionService.encryptForDatabase(attribute);
    }

    /**
     * Converts the data stored in the database column to the entity attribute value.
     * Decrypts the value after reading it from the database.
     *
     * @param dbData the data from the database column (encrypted)
     * @return the decrypted entity attribute value
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized. " +
                    "Ensure security-starter is properly configured.");
        }

        return encryptionService.decryptFromDatabase(dbData);
    }
}
