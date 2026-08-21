package id.payu.support.adapter.persistence.repository;

import id.payu.support.adapter.persistence.entity.SupportTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, UUID> {
    List<SupportTicketEntity> findByUserId(String userId);
    List<SupportTicketEntity> findByUserIdAndStatus(String userId, String status);
}
