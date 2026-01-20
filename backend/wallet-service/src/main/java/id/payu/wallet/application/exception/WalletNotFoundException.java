package id.payu.wallet.application.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String identifier) {
        super("Wallet not found: " + identifier);
    }
}
