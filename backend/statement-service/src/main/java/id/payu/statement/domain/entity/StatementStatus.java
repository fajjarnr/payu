package id.payu.statement.domain.entity;

public enum StatementStatus {
        GENERATING,   // PDF is being generated
        COMPLETED,    // PDF ready for download
        FAILED,       // Generation failed
        ARCHIVED      // Old statement, may need retrieval from archive
    }
