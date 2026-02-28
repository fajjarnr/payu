package id.payu.transaction.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating transaction tags.
 * Supports predefined categories and custom tags.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransactionTagsRequest {

    @NotEmpty(message = "Tags list cannot be empty")
    @Size(max = 10, message = "Maximum 10 tags allowed per transaction")
    private List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> tags;

    /**
     * Predefined transaction categories.
     * Users can also use custom tags beyond these predefined values.
     */
    public enum PredefinedCategory {
        FOOD_AND_DINING,
        SHOPPING,
        TRANSPORTATION,
        ENTERTAINMENT,
        BILLS_AND_UTILITIES,
        HEALTHCARE,
        EDUCATION,
        TRAVEL,
        SALARY,
        INVESTMENT,
        TRANSFER,
        GIFT,
        DONATION,
        SUBSCRIPTION,
        OTHER
    }
}
