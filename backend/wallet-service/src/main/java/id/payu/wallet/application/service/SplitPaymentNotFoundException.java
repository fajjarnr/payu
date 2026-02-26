package id.payu.wallet.application.service;

import id.payu.api.common.exception.ResourceNotFoundException;

/**
 * Exception thrown when a split payment resource is not found.
 */
public class SplitPaymentNotFoundException extends ResourceNotFoundException {

    public SplitPaymentNotFoundException(String resourceType, String identifier) {
        super(resourceType, identifier);
    }
}
