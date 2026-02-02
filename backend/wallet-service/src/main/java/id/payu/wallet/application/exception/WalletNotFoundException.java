package id.payu.wallet.application.exception;

import id.payu.api.common.exception.ResourceNotFoundException;

/**
 * Exception thrown when a wallet is not found.
 */
public class WalletNotFoundException extends ResourceNotFoundException {

    public WalletNotFoundException(String identifier) {
        super("Wallet", identifier);
    }

    public WalletNotFoundException(String code, String message) {
        super(code, message);
    }
}
