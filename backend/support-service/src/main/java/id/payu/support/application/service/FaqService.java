package id.payu.support.application.service;

import id.payu.support.domain.model.Faq;
import id.payu.support.domain.port.out.FaqRepositoryPort;
import id.payu.support.interfaces.dto.FaqResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FaqService {
    private final FaqRepositoryPort repo;
    public FaqService(FaqRepositoryPort repo) { this.repo = repo; }
    public List<FaqResponse> list(String category) {
        List<Faq> list = category == null ? repo.findAll() : repo.findByCategory(category);
        return list.stream().map(this::toResponse).toList();
    }
    private FaqResponse toResponse(Faq d){
        return new FaqResponse(d.getId(), d.getQuestion(), d.getAnswer(), d.getCategory(), d.getCreatedAt());
    }
}
