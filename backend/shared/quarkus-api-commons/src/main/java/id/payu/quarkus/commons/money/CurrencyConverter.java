package id.payu.quarkus.commons.money;

@FunctionalInterface
public interface CurrencyConverter {
    java.math.BigDecimal convert(java.math.BigDecimal amount, String fromCurrency, String toCurrency);
}
