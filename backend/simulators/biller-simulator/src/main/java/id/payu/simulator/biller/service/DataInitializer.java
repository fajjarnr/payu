package id.payu.simulator.biller.service;

import id.payu.simulator.biller.entity.BillerAccount;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import id.payu.simulator.biller.entity.AccountStatus;

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
        seed("PLN", "PLN-001234567890", "JOHN DOE", new BigDecimal("350000"), AccountStatus.ACTIVE);
        seed("PLN", "PLN-009876543210", "JANE DOE", new BigDecimal("0"), AccountStatus.ACTIVE);
        seed("PLN", "PLN-001111222233", "TEST BLOCKED USER", null, AccountStatus.BLOCKED);

        // PDAM (Water)
        seed("PDAM", "PDAM-001234567890", "JOHN DOE", new BigDecimal("125000"), AccountStatus.ACTIVE);
        seed("PDAM", "PDAM-009876543210", "JANE DOE", new BigDecimal("89000"), AccountStatus.ACTIVE);

        // Telco (Mobile)
        seed("TELKOMSEL", "081234567890", "TELKOMSEL PREPAID", null, AccountStatus.ACTIVE);
        seed("XL", "081234567891", "XL PREPAID", null, AccountStatus.ACTIVE);
        seed("INDOSAT", "081234567892", "INDOSAT PREPAID", null, AccountStatus.ACTIVE);

        // Internet
        seed("TELKOM", "TELKOM-123456789", "JOHN DOE INDIHOME", new BigDecimal("450000"), AccountStatus.ACTIVE);

        // BPJS Insurance
        seed("BPJS", "BPJS-0000123456789", "JOHN DOE", new BigDecimal("150000"), AccountStatus.ACTIVE);

        // E-wallet top-up
        seed("GOPAY", "081234567890", "GOPAY USER", null, AccountStatus.ACTIVE);
        seed("OVO", "081234567890", "OVO USER", null, AccountStatus.ACTIVE);
        seed("DANA", "081234567890", "DANA USER", null, AccountStatus.ACTIVE);
        seed("LINKAJA", "081234567890", "LINKAJA USER", null, AccountStatus.ACTIVE);

        Log.infof("Seeded %d biller accounts", BillerAccount.count());
    }

    private void seed(String billerCode, String customerNumber, String customerName,
                      BigDecimal outstanding, AccountStatus status) {
        BillerAccount account = new BillerAccount();
        account.billerCode = billerCode;
        account.customerNumber = customerNumber;
        account.customerName = customerName;
        account.outstandingAmount = outstanding;
        account.status = status;
        account.persist();
    }
}
