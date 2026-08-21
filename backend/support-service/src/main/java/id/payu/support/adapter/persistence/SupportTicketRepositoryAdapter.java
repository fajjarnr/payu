package id.payu.support.adapter.persistence;

import id.payu.support.adapter.persistence.entity.SupportTicketEntity;
import id.payu.support.adapter.persistence.repository.SupportTicketRepository;
import id.payu.support.domain.model.SupportTicket;
import id.payu.support.domain.port.out.SupportTicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupportTicketRepositoryAdapter implements SupportTicketRepositoryPort {
    private final SupportTicketRepository repository;
    @Override public SupportTicket save(SupportTicket d) {
        SupportTicketEntity e = toEntity(d);
        return toDomain(repository.save(e));
    }
    @Override public List<SupportTicket> findByUserId(String userId) {
        return repository.findByUserId(userId).stream().map(this::toDomain).toList();
    }
    @Override public List<SupportTicket> findByUserIdAndStatus(String userId, String status) {
        return repository.findByUserIdAndStatus(userId, status).stream().map(this::toDomain).toList();
    }
    @Override public Optional<SupportTicket> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }
    private SupportTicket toDomain(SupportTicketEntity e) {
        if (e==null) return null;
        SupportTicket d=new SupportTicket();
        d.setId(e.getId()); d.setTenantId(e.getTenantId()); d.setUserId(e.getUserId());
        d.setSubject(e.getSubject()); d.setDescription(e.getDescription());
        d.setCategory(e.getCategory()); d.setPriority(e.getPriority()); d.setStatus(e.getStatus());
        d.setAssignedTo(e.getAssignedTo()); d.setCreatedAt(e.getCreatedAt()); d.setUpdatedAt(e.getUpdatedAt()); d.setResolvedAt(e.getResolvedAt());
        return d;
    }
    private SupportTicketEntity toEntity(SupportTicket d) {
        if (d==null) return null;
        SupportTicketEntity e=new SupportTicketEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setUserId(d.getUserId());
        e.setSubject(d.getSubject()); e.setDescription(d.getDescription());
        e.setCategory(d.getCategory()); e.setPriority(d.getPriority()); e.setStatus(d.getStatus());
        e.setAssignedTo(d.getAssignedTo());
        return e;
    }
}
