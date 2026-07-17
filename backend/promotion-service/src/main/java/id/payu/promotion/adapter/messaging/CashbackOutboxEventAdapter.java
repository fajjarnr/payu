package id.payu.promotion.adapter.messaging;
import id.payu.outbox.service.OutboxService;
import id.payu.promotion.domain.port.out.CashbackEventPublisher;
import java.util.Map;
import org.springframework.stereotype.Component;
@Component public class CashbackOutboxEventAdapter implements CashbackEventPublisher {
 private final OutboxService outbox;
 public CashbackOutboxEventAdapter(OutboxService outbox){this.outbox=outbox;}
 public void publish(String aggregateType,String aggregateId,String eventType,Map<String,Object> payload,String topic){outbox.createEvent(aggregateType,aggregateId,eventType,payload,null,topic);}
}
