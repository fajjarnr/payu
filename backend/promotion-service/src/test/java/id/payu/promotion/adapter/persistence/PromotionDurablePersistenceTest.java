package id.payu.promotion.adapter.persistence;

import id.payu.promotion.domain.model.DiscountType;
import id.payu.promotion.domain.model.PromoStatus;
import id.payu.promotion.domain.model.UsageType;
import id.payu.promotion.domain.model.CashbackRecord;
import id.payu.promotion.domain.model.CashbackRule;
import id.payu.promotion.domain.model.PromoCode;
import id.payu.promotion.domain.model.PromoUsage;
import id.payu.promotion.adapter.persistence.repository.CashbackRecordRepository;
import id.payu.promotion.adapter.persistence.repository.CashbackRuleRepository;
import id.payu.promotion.adapter.persistence.repository.PromoCodeRepository;
import id.payu.promotion.adapter.persistence.repository.PromoUsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PromotionDurablePersistenceTest {

    @Autowired
    private PromoCodePersistenceAdapter promoCodeAdapter;

    @Autowired
    private PromoCodePersistenceMapper promoCodeMapper;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private PromoUsagePersistenceAdapter promoUsageAdapter;

    @Autowired
    private PromoUsagePersistenceMapper promoUsageMapper;

    @Autowired
    private PromoUsageRepository promoUsageRepository;

    @Autowired
    private CashbackRuleRepository cashbackRuleRepository;

    @Autowired
    private CashbackRecordRepository cashbackRecordRepository;

    @Autowired
    private CashbackRulePersistenceAdapter cashbackRuleAdapter;

    @Autowired
    private CashbackRulePersistenceMapper cashbackRuleMapper;

    @Autowired
    private CashbackRecordPersistenceAdapter cashbackRecordAdapter;

    @Autowired
    private CashbackRecordPersistenceMapper cashbackRecordMapper;

    @Test
    @Transactional
    void dataIsVisibleToASecondAdapterInstance() {
        PromoCode promo = PromoCode.builder()
                .code("DURABLE20")
                .discountValue(new BigDecimal("20"))
                .discountType(DiscountType.PERCENTAGE)
                .usageType(UsageType.ONCE_PER_USER)
                .status(PromoStatus.ACTIVE)
                .build();
        promoCodeAdapter.save(promo);

        PromoCodePersistenceAdapter secondAdapter =
                new PromoCodePersistenceAdapter(promoCodeRepository, promoCodeMapper);
        assertTrue(secondAdapter.findByCode("DURABLE20").isPresent());
    }

    @Test
    @Transactional
    void usageIsDurableAndDatabaseRejectsDuplicateUserPromoAndIdempotency() {
        PromoUsage usage = usage("user-1", "DURABLE20", "idem-1");

        assertTrue(promoUsageAdapter.recordUsage(usage));
        PromoUsagePersistenceAdapter secondAdapter =
                new PromoUsagePersistenceAdapter(promoUsageRepository, promoUsageMapper);
        assertTrue(secondAdapter.hasUserUsedPromo("user-1", "DURABLE20"));
        assertFalse(secondAdapter.recordUsage(usage("user-1", "DURABLE20", "idem-2")));
        assertFalse(secondAdapter.recordUsage(usage("user-2", "DURABLE20", "idem-1")));
    }

    @Test
    @Transactional
    void cashbackRuleAndRecordAreDurable() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE-DURABLE")
                .name("Durable rule")
                .cashbackType(id.payu.promotion.domain.model.CashbackType.FIXED)
                .cashbackAmount(new BigDecimal("100"))
                .build();
        cashbackRuleAdapter.save(rule);

        CashbackRulePersistenceAdapter secondRuleAdapter =
                new CashbackRulePersistenceAdapter(cashbackRuleRepository, cashbackRuleMapper);
        assertEquals(1, secondRuleAdapter.findActiveRules().size());
        assertEquals("RULE-DURABLE", secondRuleAdapter.findActiveRules().getFirst().getRuleId());

        CashbackRecord record = new CashbackRecord();
        record.setId(UUID.randomUUID().toString());
        record.setTransactionId("txn-durable");
        record.setAccountId("account-1");
        record.setRuleId("RULE-DURABLE");
        record.setCashbackAmount(new BigDecimal("100"));
        cashbackRecordAdapter.save(record);

        CashbackRecordPersistenceAdapter secondRecordAdapter =
                new CashbackRecordPersistenceAdapter(cashbackRecordRepository, cashbackRecordMapper);
        assertTrue(secondRecordAdapter.hasProcessedTransaction("txn-durable"));
    }

    private PromoUsage usage(String userId, String promoCode, String idempotencyKey) {
        PromoUsage usage = new PromoUsage();
        usage.setId(UUID.randomUUID().toString());
        usage.setUserId(userId);
        usage.setPromoCode(promoCode);
        usage.setTransactionId("txn-" + idempotencyKey);
        usage.setDiscountAmount(new BigDecimal("10"));
        usage.setFinalAmount(new BigDecimal("90"));
        usage.setIdempotencyKey(idempotencyKey);
        usage.setUsageType(UsageType.ONCE_PER_USER);
        return usage;
    }
}
