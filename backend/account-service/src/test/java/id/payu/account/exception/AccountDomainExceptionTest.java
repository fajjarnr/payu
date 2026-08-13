package id.payu.account.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ACCOUNT-006: AccountDomainException hierarchy coverage.
 */
@DisplayName("AccountDomainException hierarchy")
class AccountDomainExceptionTest {

    @Test
    void invalidPhoneNumber() {
        AccountDomainException.InvalidPhoneNumberException ex =
                new AccountDomainException.InvalidPhoneNumberException("0812");
        assertThat(ex.getCode()).isNotBlank();
        assertThat(ex.getMessage()).contains("0812");
    }

    @Test
    void invalidEmail() {
        AccountDomainException.InvalidEmailException ex =
                new AccountDomainException.InvalidEmailException("x@y");
        assertThat(ex.getCode()).isNotBlank();
    }

    @Test
    void invalidNik() {
        AccountDomainException.InvalidNikException ex =
                new AccountDomainException.InvalidNikException("3201");
        assertThat(ex.getCode()).isNotBlank();
    }

    @Test
    void accountAlreadyExists() {
        AccountDomainException.AccountAlreadyExistsException ex =
                new AccountDomainException.AccountAlreadyExistsException("0812");
        assertThat(ex.getCode()).isNotBlank();
    }

    @Test
    void accountNotActiveAndKycAndBlocked() {
        assertThat(new AccountDomainException.AccountNotActiveException("acc-1").getCode()).isNotBlank();
        assertThat(new AccountDomainException.KycNotVerifiedException("acc-1").getCode()).isNotBlank();
        assertThat(new AccountDomainException.AccountBlockedException("acc-1").getCode()).isNotBlank();
    }

    @Test
    void dukcapilVerificationFailed() {
        AccountDomainException.DukcapilVerificationFailedException ex =
                new AccountDomainException.DukcapilVerificationFailedException("dukcapil down");
        assertThat(ex.getCode()).isNotBlank();
    }

    @Test
    void dukcapilServiceUnavailable() {
        assertThat(new AccountDomainException.DukcapilServiceUnavailableException().getCode()).isNotBlank();
        assertThat(new AccountDomainException.DukcapilServiceUnavailableException(
                new RuntimeException("down")).getCode()).isNotBlank();
    }

    @Test
    void accountCreationFailed() {
        assertThat(new AccountDomainException.AccountCreationFailedException("dup").getCode()).isNotBlank();
        assertThat(new AccountDomainException.AccountCreationFailedException(
                "dup", new RuntimeException("down")).getMessage()).contains("dup");
    }

    @Test
    void argConstructorCoveredBySubclass() {
        // exercises the (code, message, Object... args) super constructor
        AccountDomainException ex = new AccountDomainException("ACC_X", "code {}") {
        };
        assertThat(ex.getCode()).isEqualTo("ACC_X");
    }
}
