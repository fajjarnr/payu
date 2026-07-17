package id.payu.promotion.domain.model;
public record CashbackSagaOutcome(boolean success,boolean compensated,Cashback cashback,String error,String errorStep,String state) {}
