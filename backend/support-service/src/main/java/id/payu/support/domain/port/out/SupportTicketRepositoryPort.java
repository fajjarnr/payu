package id.payu.support.domain.port.out;

import id.payu.support.domain.model.SupportTicket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepositoryPort {
    SupportTicket save(SupportTicket ticket);
    List<SupportTicket> findByUserId(String userId);
    List<SupportTicket> findByUserIdAndStatus(String userId, String status);
    Optional<SupportTicket> findById(UUID id);
}
