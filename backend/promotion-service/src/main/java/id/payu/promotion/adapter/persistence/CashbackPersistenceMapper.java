package id.payu.promotion.adapter.persistence;
import id.payu.promotion.adapter.persistence.entity.CashbackEntity;
import id.payu.promotion.domain.model.Cashback;
import org.springframework.stereotype.Component;
@Component public class CashbackPersistenceMapper {
 public Cashback toDomain(CashbackEntity e){ Cashback c=new Cashback(); c.setId(e.getId()); c.setAccountId(e.getAccountId()); c.setTransactionId(e.getTransactionId()); c.setCashbackAmount(e.getCashbackAmount()); c.setTransactionAmount(e.getTransactionAmount()); c.setPercentage(e.getPercentage()); c.setMerchantCode(e.getMerchantCode()); c.setCategoryCode(e.getCategoryCode()); c.setCashbackCode(e.getCashbackCode()); c.setStatus(e.getStatus()); c.setCreditedAt(e.getCreditedAt()); c.setExpiryDate(e.getExpiryDate()); c.setCreatedAt(e.getCreatedAt()); return c; }
 public CashbackEntity toEntity(Cashback c){ CashbackEntity e=new CashbackEntity(); e.setId(c.getId()); e.setAccountId(c.getAccountId()); e.setTransactionId(c.getTransactionId()); e.setCashbackAmount(c.getCashbackAmount()); e.setTransactionAmount(c.getTransactionAmount()); e.setPercentage(c.getPercentage()); e.setMerchantCode(c.getMerchantCode()); e.setCategoryCode(c.getCategoryCode()); e.setCashbackCode(c.getCashbackCode()); e.setStatus(c.getStatus()); e.setCreditedAt(c.getCreditedAt()); e.setExpiryDate(c.getExpiryDate()); e.setCreatedAt(c.getCreatedAt()); return e; }
}
