package id.payu.backoffice.application.service;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import id.payu.backoffice.dto.KycReviewDecisionRequest;
import id.payu.backoffice.dto.KycReviewRequest;
import id.payu.backoffice.adapter.persistence.repository.KycReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.KycStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycReviewService {

    private final KycReviewRepository repository;

    @Transactional
    public KycReviewEntity create(KycReviewRequest request) {
        log.info("Creating KYC review for user: {}", request.userId());

        KycReviewEntity review = KycReviewEntity.builder()
                .userId(request.userId())
                .accountNumber(request.accountNumber())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .documentUrl(request.documentUrl())
                .fullName(request.fullName())
                .address(request.address())
                .phoneNumber(request.phoneNumber())
                .notes(request.notes())
                .status(KycStatus.PENDING)
                .build();

        KycReviewEntity saved = repository.save(review);
        log.info("KYC review created: id={}", saved.getId());
        return saved;
    }

    public Optional<KycReviewEntity> getById(UUID id) {
        return repository.findById(id);
    }

    public Optional<KycReviewEntity> getByUserId(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().findFirst();
    }

    public List<KycReviewEntity> listByStatus(KycStatus status, int page, int size) {
        // BUG-BE-043: Use DB-level pagination instead of ignoring page/size
        return repository.findByStatus(status, PageRequest.of(page, size)).getContent();
    }

    public List<KycReviewEntity> listAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public KycReviewEntity review(UUID id, KycReviewDecisionRequest request, String reviewedBy) {
        log.info("Reviewing KYC: id={}, status={}, reviewer={}", id, request.status(), reviewedBy);

        KycReviewEntity review = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KYC review not found: " + id));

        review.setStatus(switch (request.status()) {
            case APPROVED -> KycStatus.APPROVED;
            case REJECTED -> KycStatus.REJECTED;
            case REQUIRES_ADDITIONAL_INFO -> KycStatus.REQUIRES_ADDITIONAL_INFO;
        });

        review.setNotes(request.notes());
        review.setReviewedBy(reviewedBy);
        review.setReviewedAt(LocalDateTime.now());

        KycReviewEntity saved = repository.save(review);
        log.info("KYC review updated: id={}, newStatus={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting KYC review: id={}", id);
        repository.deleteById(id);
    }
}
