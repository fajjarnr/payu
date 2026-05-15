package id.payu.simulator.qris.service;

import id.payu.simulator.qris.entity.Merchant;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import id.payu.simulator.qris.entity.MerchantCategory;
import id.payu.simulator.qris.entity.MerchantStatus;

/**
 * Initializes test merchant data on application startup.
 */
@ApplicationScoped
public class DataInitializer {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        Log.info("Initializing test merchant data...");

        if (Merchant.count() > 0) {
            Log.info("Test merchants already exist, skipping initialization");
            return;
        }

        // Food & Beverage
        createMerchant("MCH001", "Warung Makan Sederhana", "JAKARTA", 
                       MerchantCategory.FOOD_BEVERAGE, "ID1234567890");
        createMerchant("MCH002", "Kopi Kenangan", "JAKARTA", 
                       MerchantCategory.FOOD_BEVERAGE, "ID1234567891");
        createMerchant("MCH003", "Bakso Pak Kumis", "BANDUNG", 
                       MerchantCategory.FOOD_BEVERAGE, "ID1234567892");

        // Retail
        createMerchant("MCH010", "Toko Serba Ada", "SURABAYA", 
                       MerchantCategory.RETAIL, "ID2234567890");
        createMerchant("MCH011", "Minimart 24 Jam", "JAKARTA", 
                       MerchantCategory.RETAIL, "ID2234567891");

        // Electronics
        createMerchant("MCH020", "Toko Elektronik Jaya", "BANDUNG", 
                       MerchantCategory.ELECTRONICS, "ID3234567890");
        createMerchant("MCH021", "Gadget Store Indonesia", "JAKARTA", 
                       MerchantCategory.ELECTRONICS, "ID3234567891");

        // Health
        createMerchant("MCH030", "Apotek Sehat Selalu", "SURABAYA", 
                       MerchantCategory.HEALTH, "ID4234567890");
        createMerchant("MCH031", "Klinik Dokter Keluarga", "YOGYAKARTA", 
                       MerchantCategory.HEALTH, "ID4234567891");

        // Fashion
        createMerchant("MCH040", "Butik Cantik", "JAKARTA", 
                       MerchantCategory.FASHION, "ID5234567890");

        // Transport
        createMerchant("MCH050", "Parkir Mall Central", "JAKARTA", 
                       MerchantCategory.TRANSPORT, "ID6234567890");

        // Entertainment
        createMerchant("MCH060", "Bioskop XXI", "JAKARTA", 
                       MerchantCategory.ENTERTAINMENT, "ID7234567890");

        // Blocked merchant (for testing)
        Merchant blockedMerchant = new Merchant();
        blockedMerchant.merchantId = "MCH999";
        blockedMerchant.merchantName = "Test Blocked Merchant";
        blockedMerchant.merchantCity = "UNKNOWN";
        blockedMerchant.merchantCategory = MerchantCategory.OTHER;
        blockedMerchant.status = MerchantStatus.BLOCKED;
        blockedMerchant.persist();

        // Suspended merchant
        Merchant suspendedMerchant = new Merchant();
        suspendedMerchant.merchantId = "MCH998";
        suspendedMerchant.merchantName = "Test Suspended Merchant";
        suspendedMerchant.merchantCity = "UNKNOWN";
        suspendedMerchant.merchantCategory = MerchantCategory.OTHER;
        suspendedMerchant.status = MerchantStatus.SUSPENDED;
        suspendedMerchant.persist();

        Log.infof("Initialized %d test merchants", Merchant.count());
    }

    private void createMerchant(String merchantId, String merchantName, String city,
                                 MerchantCategory category, String nmid) {
        Merchant merchant = new Merchant();
        merchant.merchantId = merchantId;
        merchant.merchantName = merchantName;
        merchant.merchantCity = city;
        merchant.merchantCategory = category;
        merchant.nmid = nmid;
        merchant.terminalId = "T" + merchantId.substring(3);
        merchant.status = MerchantStatus.ACTIVE;
        merchant.persist();

        Log.debugf("Created merchant: %s - %s (%s)", merchantId, merchantName, category);
    }
}
