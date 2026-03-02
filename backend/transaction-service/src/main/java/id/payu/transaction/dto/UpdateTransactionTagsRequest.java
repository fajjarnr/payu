package id.payu.transaction.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for updating transaction tags.
 * Supports predefined categories and custom tags.
 */
public class UpdateTransactionTagsRequest {
    public UpdateTransactionTagsRequest() {
    }

    public UpdateTransactionTagsRequest(List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> tags) {
        this.tags = tags;
    }

    public static UpdateTransactionTagsRequestBuilder builder() {
        return new UpdateTransactionTagsRequestBuilder();
    }

    public static class UpdateTransactionTagsRequestBuilder {
        private List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> tags;

        public UpdateTransactionTagsRequestBuilder tags(List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> tags) {
            this.tags = tags;
            return this;
        }

        public UpdateTransactionTagsRequest build() {
            return new UpdateTransactionTagsRequest(tags);
        }
    }

    public List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> getTags() {
        return tags;
    }

    public void setTags(List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> tags) {
        this.tags = tags;
    }



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
