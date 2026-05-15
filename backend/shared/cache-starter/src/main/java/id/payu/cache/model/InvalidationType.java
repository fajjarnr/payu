package id.payu.cache.model;

public enum InvalidationType {
        /**
         * Invalidate a single cache key.
         */
        KEY,

        /**
         * Invalidate keys matching a pattern.
         */
        PATTERN,

        /**
         * Invalidate all keys in a cache.
         */
        ALL
    }
