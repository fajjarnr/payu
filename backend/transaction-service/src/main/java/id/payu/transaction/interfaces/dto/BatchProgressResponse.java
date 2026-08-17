package id.payu.transaction.interfaces.dto;

import java.util.UUID;

/**
 * Response DTO for batch progress.
 */
public class BatchProgressResponse {
    public BatchProgressResponse() {
    }

    public BatchProgressResponse(UUID batchId, int progressPercentage) {
        this.batchId = batchId;
        this.progressPercentage = progressPercentage;
    }

    public static BatchProgressResponseBuilder builder() {
        return new BatchProgressResponseBuilder();
    }

    public static class BatchProgressResponseBuilder {
        private UUID batchId;
        private int progressPercentage;

        public BatchProgressResponseBuilder batchId(UUID batchId) {
            this.batchId = batchId;
            return this;
        }
        public BatchProgressResponseBuilder progressPercentage(int progressPercentage) {
            this.progressPercentage = progressPercentage;
            return this;
        }

        public BatchProgressResponse build() {
            return new BatchProgressResponse(batchId, progressPercentage);
        }
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }



    private UUID batchId;
    private int progressPercentage;
}
