package id.payu.support.adapter.persistence;

import id.payu.support.adapter.persistence.entity.FaqEntity;
import id.payu.support.adapter.persistence.repository.FaqRepository;
import id.payu.support.domain.model.Faq;
import id.payu.support.domain.port.out.FaqRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FaqRepositoryAdapter implements FaqRepositoryPort {
    private final FaqRepository repository;
    @Override public List<Faq> findAll() { return repository.findAll().stream().map(this::toDomain).toList(); }
    @Override public List<Faq> findByCategory(String c) { return repository.findByCategory(c).stream().map(this::toDomain).toList(); }
    private Faq toDomain(FaqEntity e){ Faq d=new Faq(); d.setId(e.getId()); d.setQuestion(e.getQuestion()); d.setAnswer(e.getAnswer()); d.setCategory(e.getCategory()); d.setCreatedAt(e.getCreatedAt()); return d; }
}
