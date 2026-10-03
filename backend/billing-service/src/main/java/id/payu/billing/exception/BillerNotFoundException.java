package id.payu.billing.exception;

import id.payu.api.common.exception.ResourceNotFoundException;

public class BillerNotFoundException extends ResourceNotFoundException {

    public BillerNotFoundException(String message) {
        super("BILLER_NOT_FOUND", message);
    }

    public BillerNotFoundException(String billerCode, String message) {
        super("BILLER_NOT_FOUND", message);
    }
}
