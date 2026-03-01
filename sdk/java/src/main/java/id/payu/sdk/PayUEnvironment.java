package id.payu.sdk;

/**
 * Environment configuration for PayU API.
 */
public enum PayUEnvironment {
    /**
     * Sandbox environment for testing.
     * No real money is moved.
     */
    SANDBOX("https://sandbox-api.payu.fajjjar.my.id"),

    /**
     * Production environment for live transactions.
     */
    PRODUCTION("https://api.payu.fajjjar.my.id");

    private final String baseUrl;

    PayUEnvironment(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Get the base URL for this environment.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}
