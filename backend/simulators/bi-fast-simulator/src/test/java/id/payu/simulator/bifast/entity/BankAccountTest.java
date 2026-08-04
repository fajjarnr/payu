package id.payu.simulator.bifast.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BankAccountTest {

    @Test
    void mapsNumericBankCodesToSimulatorAliases() {
        assertEquals("BCA", BankAccount.canonicalBankCode("014"));
        assertEquals("BRI", BankAccount.canonicalBankCode("002"));
        assertEquals("BCA", BankAccount.canonicalBankCode("BCA"));
    }
}
