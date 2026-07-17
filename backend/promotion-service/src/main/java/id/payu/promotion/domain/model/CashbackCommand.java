package id.payu.promotion.domain.model;
import java.math.BigDecimal;
public record CashbackCommand(String accountId,String transactionId,BigDecimal transactionAmount,String merchantCode,String categoryCode,String cashbackCode) {}
