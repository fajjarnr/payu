package id.payu.simulator.biller.service;

import id.payu.simulator.biller.entity.BillerAccount;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

/**
 * Seeds test biller accounts on application startup.
 * Only seeds if database is empty (idempotent).
 */
@ApplicationScoped
public class DataInitializer {

    @Transactional
    void onStartup(@Observes StartupEvent event) {
        if (BillerAccount.count() > 0) {
            Log.info("Database already seeded, skipping initialization");
            return;
        }

        Log.info("Seeding biller test accounts...");

        // PLN (Electricity)
        seed("PLN", "PLN-001234567890", "JOHN DOE", new BigDecimal("350000"), BillerAccount.AccountStatus.ACTIVE);
        seed("PLN", "PLN-009876543210", "JANE DOE", new BigDecimal("0"), BillerAccount.AccountStatus.ACTIVE);
        seed("PLN", "PLN-001111222233", "TEST BLOCKED USER", null, BillerAccount.AccountStatus.BLOCKED);

        // PDAM (Water)
        seed("PDAM", "PDAM-001234567890", "JOHN DOE", new BigDecimal("125000"), BillerAccount.AccountStatus.ACTIVE);
        seed("PDAM", "PDAM-009876543210", "JANE DOE", new BigDecimal("89000"), BillerAccount.AccountStatus.ACTIVE);

        // Telco (Mobile)
        seed("TELKOMSEL", "081234567890", "TELKOMSEL PREPAID", null, BillerAccount.AccountStatus.ACTIVE);
        seed("XL", "081234567891", "XL PREPAID", null, BillerAccount.AccountStatus.ACTIVE);
        seed("INDOSAT", "081234567892", "INDOSAT PREPAID", null, BillerAccount.AccountStatus.ACTIVE);

        // Internet
        seed("TELKOM", "TELKOM-123456789", "JOHN DOE INDIHOME", new BigDecimal("450000"), BillerAccount.AccountStatus.ACTIVE);

        // BPJS Insurance
        seed("BPJS", "BPJS-0000123456789", "JOHN DOE", new BigDecimal("150000"), BillerAccount.AccountStatus.ACTIVE);

        // E-wallet top-up
        seed("GOPAY", "081234567890", "GOPAY USER", null, BillerAccount.AccountStatus.ACTIVE);
        seed("OVO", "081234567890", "OVO USER", null, BillerAccount.AccountStatus.ACTIVE);
        seed("DANA", "081234567890", "DANA USER", null, BillerAccount.AccountStatus.ACTIVE);
        seed("LINKAJA", "081234567890", "LINKAJA USER", null, BillerAccount.AccountStatus.ACTIVE);

        Log.infof("Seeded %d biller accounts", BillerAccount.count());
    }

    private void seed(String billerCode, String customerNumber, String customerName,
                      BigDecimal outstanding, BillerAccount.AccountStatus status) {
        BillerAccount account = new BillerAccount();
        account.billerCode = billerCode;
        account.customerNumber = customerNumber;
        account.customerName = customerName;
        account.outstandingAmount = outstanding;
        account.status = status;
        account.persist();
    }
}
