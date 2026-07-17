package id.payu.promotion.adapter.persistence;
import id.payu.promotion.adapter.persistence.repository.PromotionRepository; import id.payu.promotion.domain.*; import id.payu.promotion.domain.model.Promotion; import id.payu.promotion.domain.port.out.PromotionPersistencePort; import jakarta.persistence.EntityManager; import java.time.LocalDateTime; import java.util.*; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;
@Component public class PromotionPersistenceAdapter implements PromotionPersistencePort {
 private final PromotionRepository repo; private final PromotionPersistenceMapper mapper; private final EntityManager em;
 public PromotionPersistenceAdapter(PromotionRepository r,PromotionPersistenceMapper m,EntityManager e){repo=r;mapper=m;em=e;}
 public Promotion save(Promotion p){return mapper.toDomain(repo.save(mapper.toEntity(p)));} public Optional<Promotion> findById(UUID id){return repo.findById(id).map(mapper::toDomain);} public Optional<Promotion> findByCode(String c){return repo.findByCode(c).map(mapper::toDomain);} public List<Promotion> findActivePromotions(PromotionStatus s,LocalDateTime n){return repo.findActivePromotions(s,n).stream().map(mapper::toDomain).toList();}
 @Transactional public Optional<Promotion> incrementRedemptionIfAvailable(UUID id){if(repo.atomicIncrementRedemptionCount(id)==0)return Optional.empty();em.flush();em.clear();return repo.findById(id).map(mapper::toDomain);}
}
