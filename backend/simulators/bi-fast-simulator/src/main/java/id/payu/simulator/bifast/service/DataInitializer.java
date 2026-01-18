package id.payu.simulator.bifast.service;

import id.payu.simulator.bifast.entity.BankAccount;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

/**
 * Initializes test bank accounts on application startup.
 */
@ApplicationScoped
public class DataInitializer {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        Log.info("Initializing test bank accounts...");

        // Only initialize if no accounts exist
        if (BankAccount.count() > 0) {
            Log.info("Test accounts already exist, skipping initialization");
            return;
        }

        createAccount("BCA", "1234567890", "JOHN DOE", BankAccount.AccountStatus.ACTIVE);
        createAccount("BCA", "1234567891", "JANE SMITH", BankAccount.AccountStatus.ACTIVE);
        createAccount("BCA", "1234567892", "ALICE WONG", BankAccount.AccountStatus.ACTIVE);
        
        createAccount("BRI", "0987654321", "JANE DOE", BankAccount.AccountStatus.ACTIVE);
        createAccount("BRI", "0987654322", "BOB JOHNSON", BankAccount.AccountStatus.ACTIVE);
        createAccount("BRI", "0987654323", "CAROL DAVIS", BankAccount.AccountStatus.ACTIVE);
        
        createAccount("MANDIRI", "1111222233", "TEST BLOCKED", BankAccount.AccountStatus.BLOCKED);
        createAccount("MANDIRI", "1111222234", "DAVID LEE", BankAccount.AccountStatus.ACTIVE);
        createAccount("MANDIRI", "1111222235", "EVE BROWN", BankAccount.AccountStatus.ACTIVE);
        
        createAccount("BNI", "9999888877", "TEST TIMEOUT", BankAccount.AccountStatus.TIMEOUT);
        createAccount("BNI", "9999888878", "FRANK MILLER", BankAccount.AccountStatus.ACTIVE);
        createAccount("BNI", "9999888879", "GRACE TAYLOR", BankAccount.AccountStatus.ACTIVE);
        
        createAccount("CIMB", "5555666677", "HENRY WILSON", BankAccount.AccountStatus.ACTIVE);
        createAccount("DANAMON", "4444333322", "IVAN CHEN", BankAccount.AccountStatus.ACTIVE);
        createAccount("PERMATA", "6666777788", "JULIA ANDERSON", BankAccount.AccountStatus.ACTIVE);
        createAccount("OCBC", "7777888899", "KEVIN THOMAS", BankAccount.AccountStatus.DORMANT);

        Log.infof("Initialized %d test bank accounts", BankAccount.count());
    }

    private void createAccount(String bankCode, String accountNumber, String accountName, 
                               BankAccount.AccountStatus status) {
        BankAccount account = new BankAccount();
        account.bankCode = bankCode;
        account.accountNumber = accountNumber;
        account.accountName = accountName;
        account.status = status;
        account.persist();
        
        Log.debugf("Created account: %s-%s (%s) - %s", 
                   bankCode, accountNumber, accountName, status);
    }
}
