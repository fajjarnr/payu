package id.payu.promotion.adapter.persistence;
import id.payu.promotion.adapter.persistence.repository.CashbackRepository;
import id.payu.promotion.domain.model.Cashback;
import id.payu.promotion.domain.port.out.CashbackPersistencePort;
import java.util.*;
import org.springframework.stereotype.Component;
@Component public class CashbackPersistenceAdapter implements CashbackPersistencePort {
 private final CashbackRepository repository; private final CashbackPersistenceMapper mapper;
 public CashbackPersistenceAdapter(CashbackRepository r,CashbackPersistenceMapper m){repository=r;mapper=m;}
 public Cashback save(Cashback c){return mapper.toDomain(repository.save(mapper.toEntity(c)));}
 public Optional<Cashback> findById(UUID id){return repository.findById(id).map(mapper::toDomain);}
 public Optional<Cashback> findByTransactionId(String transactionId){return repository.findByTransactionId(transactionId).map(mapper::toDomain);}
 public List<Cashback> findByAccountId(String id){return repository.findByAccountId(id).stream().map(mapper::toDomain).toList();}
}
