package id.payu.transaction.application.service.dto;

public class ArchivalResult {
    public ArchivalResult() {
    }

    public ArchivalResult(int archivedCount, Long batchId, String status) {
        this.archivedCount = archivedCount;
        this.batchId = batchId;
        this.status = status;
    }

    public static ArchivalResultBuilder builder() {
        return new ArchivalResultBuilder();
    }

    public static class ArchivalResultBuilder {
        private int archivedCount;
        private Long batchId;
        private String status;

        public ArchivalResultBuilder archivedCount(int archivedCount) {
            this.archivedCount = archivedCount;
            return this;
        }
        public ArchivalResultBuilder batchId(Long batchId) {
            this.batchId = batchId;
            return this;
        }
        public ArchivalResultBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ArchivalResult build() {
            return new ArchivalResult(archivedCount, batchId, status);
        }
    }

    public int getArchivedCount() {
        return archivedCount;
    }

    public void setArchivedCount(int archivedCount) {
        this.archivedCount = archivedCount;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    private int archivedCount;
    private Long batchId;
    private String status;
}
