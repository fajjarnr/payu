package id.payu.promotion.domain.port.out; import java.util.Map;
public interface DomainEventPublisher { void createEvent(String aggregate,String id,String type,Map<String,Object> payload,Map<String,Object> metadata,String topic); }
