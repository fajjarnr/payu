package id.payu.productcatalog.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for getting a specific product parameter.
 */
public class ProductParameterRequest {

    @NotBlank(message = "Parameter key is required")
    private String key;

    private Object defaultValue;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
}
