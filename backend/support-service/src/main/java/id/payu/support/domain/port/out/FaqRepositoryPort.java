package id.payu.support.domain.port.out;

import id.payu.support.domain.model.Faq;
import java.util.List;

public interface FaqRepositoryPort {
    List<Faq> findAll();
    List<Faq> findByCategory(String category);
}
