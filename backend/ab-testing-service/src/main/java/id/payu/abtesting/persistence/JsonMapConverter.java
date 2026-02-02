package id.payu.abtesting.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA AttributeConverter for Map<String, Object> to PostgreSQL JSONB
 */
@Converter(autoApply = false)
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not convert map to JSON string", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not convert JSON string to map", e);
        }
    }
}
