package id.payu.transaction.domain.model;

public enum BankCode {
        BCA("BCA", "002", "1234"),
        BNI("BNI", "009", "8800"),
        MANDIRI("MANDIRI", "008", "7700"),
        PERMATA("PERMATA", "013", "5500");

        private final String name;
        private final String code;
        private final String prefix;

        BankCode(String name, String code, String prefix) {
            this.name = name;
            this.code = code;
            this.prefix = prefix;
        }

        public String getBankName() { return name; }
        public String getBankCode() { return code; }
        public String getPrefix() { return prefix; }

        public static BankCode fromCode(String code) {
            for (BankCode bc : values()) {
                if (bc.name().equalsIgnoreCase(code) || bc.code.equals(code)) {
                    return bc;
                }
            }
            throw new IllegalArgumentException("Unknown bank code: " + code);
        }
    }
