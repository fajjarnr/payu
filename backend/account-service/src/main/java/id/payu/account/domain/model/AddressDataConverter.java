package id.payu.account.domain.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.account.adapter.persistence.entity.SensitiveUserDataEntity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for SensitiveUserDataEntity.AddressData to JSONB
 * Handles serialization/deserialization between AddressData object and JSON string
 */
@Converter(autoApply = false)
public class AddressDataConverter implements AttributeConverter<SensitiveUserDataEntity.AddressData, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(SensitiveUserDataEntity.AddressData addressData) {
        if (addressData == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(addressData);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting AddressData to JSON", e);
        }
    }

    @Override
    public SensitiveUserDataEntity.AddressData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<SensitiveUserDataEntity.AddressData>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to AddressData", e);
        }
    }
}
