package id.payu.commons.idempotency;

public enum EntryStatus {
        /**
         * Request is currently being processed (in-flight).
         */
        IN_PROGRESS,

        /**
         * Request has been completed successfully.
         */
        COMPLETED,

        /**
         * Request failed with an error.
         */
        FAILED
    }
