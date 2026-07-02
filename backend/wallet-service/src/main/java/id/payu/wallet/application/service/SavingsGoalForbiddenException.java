package id.payu.wallet.application.service;

import id.payu.api.common.exception.BusinessException;

public class SavingsGoalForbiddenException extends BusinessException {
    public SavingsGoalForbiddenException(String message) {
        super("SAV_403", message);
    }
}
