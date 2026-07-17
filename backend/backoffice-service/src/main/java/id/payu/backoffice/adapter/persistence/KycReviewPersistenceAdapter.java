package id.payu.backoffice.adapter.persistence;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import id.payu.backoffice.adapter.persistence.repository.KycReviewRepository;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.domain.KycStatus;
import id.payu.backoffice.domain.port.outbound.KycReviewRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KycReviewPersistenceAdapter implements KycReviewRepositoryPort {
    private final KycReviewRepository repository;
    private final KycReviewMapper mapper;
    private final BlindIndexService blindIndex;

    private List<KycReview> map(List<KycReviewEntity> values) {
        return values.stream().map(mapper::toDomain).toList();
    }

    public KycReview save(KycReview value) {
        KycReviewEntity entity = mapper.toEntity(value);
        entity.setUserIdBlindIndex(blindIndex.index(value.getUserId()));
        entity.setUserIdBlindIndexKeyVersion(blindIndex.currentVersion());
        return mapper.toDomain(repository.save(entity));
    }

    public Optional<KycReview> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public Optional<KycReview> findLatestByUserId(String id) {
        return repository.findByUserIdBlindIndexInOrderByCreatedAtDesc(blindIndex.lookupIndexes(id)).stream()
                .findFirst().map(mapper::toDomain);
    }
    public List<KycReview> findByStatus(KycStatus status, int page, int size) {
        return repository.findByStatus(status, PageRequest.of(page, size)).map(mapper::toDomain).getContent();
    }
    public List<KycReview> findAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).map(mapper::toDomain).getContent();
    }
    public List<KycReview> findByUserIdContainingIgnoreCase(String query) { return List.of(); }
    public List<KycReview> findByAccountNumberContainingIgnoreCase(String query) { return List.of(); }
    public List<KycReview> findByDocumentNumberContainingIgnoreCase(String query) { return List.of(); }
    public List<KycReview> findByFullNameContainingIgnoreCase(String query) { return List.of(); }
    public void deleteById(UUID id) { repository.deleteById(id); }
}
