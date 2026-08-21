package id.payu.support.application.service;

import id.payu.support.domain.model.SupportTicket;
import id.payu.support.domain.port.out.SupportTicketRepositoryPort;
import id.payu.support.interfaces.dto.SupportTicketResponse;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class SupportTicketService {
    private final SupportTicketRepositoryPort repo;
    public SupportTicketService(SupportTicketRepositoryPort repo) { this.repo = repo; }
    public SupportTicketResponse create(String userId, Map<String,String> body) {
        SupportTicket d = new SupportTicket();
        d.setUserId(userId);
        d.setTenantId("payu");
        d.setSubject(body.getOrDefault("subject","No subject"));
        d.setDescription(body.getOrDefault("description",""));
        d.setCategory(body.getOrDefault("category","OTHER"));
        d.setPriority(body.getOrDefault("priority","MEDIUM"));
        d.setStatus("OPEN");
        SupportTicket saved = repo.save(d);
        return toResponse(saved);
    }
    public List<SupportTicketResponse> list(String userId, String status) {
        List<SupportTicket> list = status == null ? repo.findByUserId(userId) : repo.findByUserIdAndStatus(userId, status);
        return list.stream().map(this::toResponse).toList();
    }
    private SupportTicketResponse toResponse(SupportTicket d){
        return new SupportTicketResponse(d.getId(), d.getTenantId(), d.getUserId(), d.getSubject(), d.getDescription(), d.getCategory(), d.getPriority(), d.getStatus(), d.getAssignedTo(), d.getCreatedAt(), d.getUpdatedAt(), d.getResolvedAt());
    }
}
