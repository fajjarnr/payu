package id.payu.account.adapter.persistence;

import id.payu.account.adapter.persistence.entity.SensitiveUserDataEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ACCOUNT-006: AddressDataConverter coverage.
 */
@DisplayName("AddressDataConverter")
class AddressDataConverterTest {

    private final AddressDataConverter converter = new AddressDataConverter();

    private SensitiveUserDataEntity.AddressData addressData() {
        SensitiveUserDataEntity.AddressData a = new SensitiveUserDataEntity.AddressData();
        a.setStreet("Jl. Test");
        a.setCity("JAKARTA");
        a.setPostalCode("12345");
        return a;
    }

    @Test
    void convertToDatabaseColumnRoundTrips() {
        SensitiveUserDataEntity.AddressData data = addressData();

        String json = converter.convertToDatabaseColumn(data);
        SensitiveUserDataEntity.AddressData back = converter.convertToEntityAttribute(json);

        assertThat(back.getCity()).isEqualTo("JAKARTA");
        assertThat(back.getStreet()).isEqualTo("Jl. Test");
    }

    @Test
    void convertNullAndBlankValues() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("  ")).isNull();
    }
}
