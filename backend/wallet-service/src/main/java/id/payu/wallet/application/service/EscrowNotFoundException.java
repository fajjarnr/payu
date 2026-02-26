package id.payu.wallet.application.service;

import id.payu.api.common.exception.ResourceNotFoundException;

/**
 * Exception thrown when an escrow transaction is not found.
 */
public class EscrowNotFoundException extends ResourceNotFoundException {

    public EscrowNotFoundException(String identifier) {
        super("EscrowTransaction", identifier);
    }
}
