package id.payu.gateway.domain.vo;

import java.util.Objects;

public enum MaskingStrategy {
        FULL {
            @Override
            public String mask(String value, String pattern) {
                return pattern != null ? pattern : "***";
            }
        },
        PARTIAL {
            @Override
            public String mask(String value, String pattern) {
                if (value == null || value.length() <= 4) {
                    return "***";
                }
                // Show first 2 and last 2 characters
                return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
            }
        },
        LAST_4 {
            @Override
            public String mask(String value, String pattern) {
                if (value == null || value.length() <= 4) {
                    return "***";
                }
                return "****" + value.substring(value.length() - 4);
            }
        },
        HASH {
            @Override
            public String mask(String value, String pattern) {
                return "[HASH:" + Integer.toHexString(Objects.hash(value)) + "]";
            }
        };

        public abstract String mask(String value, String pattern);
    }
