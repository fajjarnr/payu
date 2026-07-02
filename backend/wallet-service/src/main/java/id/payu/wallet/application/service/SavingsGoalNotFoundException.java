package id.payu.wallet.application.service;

import id.payu.api.common.exception.BusinessException;

public class SavingsGoalNotFoundException extends BusinessException {
    public SavingsGoalNotFoundException(String message) {
        super("SAV_001", message);
    }
}
