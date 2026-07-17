package id.payu.promotion.domain.port.out;
import id.payu.promotion.domain.*; import id.payu.promotion.domain.model.Promotion; import java.time.LocalDateTime; import java.util.*;
public interface PromotionPersistencePort { Promotion save(Promotion p); Optional<Promotion> findById(UUID id); Optional<Promotion> findByCode(String code); List<Promotion> findActivePromotions(PromotionStatus status,LocalDateTime now); Optional<Promotion> incrementRedemptionIfAvailable(UUID id); }
