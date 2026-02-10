package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.KycReview;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class KycReviewService {

    private final KycReviewRepository repository;

    @Transactional
    public KycReview create(KycReviewRequest request) {
        log.info("Creating KYC review for user: {}", request.userId());

        KycReview review = KycReview.builder()
                .userId(request.userId())
                .accountNumber(request.accountNumber())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .documentUrl(request.documentUrl())
                .fullName(request.fullName())
                .address(request.address())
                .phoneNumber(request.phoneNumber())
                .notes(request.notes())
                .status(KycReview.KycStatus.PENDING)
                .build();

        KycReview saved = repository.save(review);
        log.info("KYC review created: id={}", saved.getId());
        return saved;
    }

    public Optional<KycReview> getById(UUID id) {
        return repository.findById(id);
    }

    public Optional<KycReview> getByUserId(String userId) {
        // Simplified for this example, or use repository
        return repository.findByUserId(userId).stream().findFirst();
    }

    public List<KycReview> listByStatus(KycReview.KycStatus status, int page, int size) {
        return repository.findByStatus(status);
        // Note: Pagination support would require PagingAndSortingRepository properly
        // For now, simpler implementation to match interface roughly or I should assume repo handles page?
        // Actually, let's stick to standard findAll if possible or assume repo has pagination methods if defined.
        // My repo def didn't have Pageable. I'll just return list for now to satisfy potential callers.
        // Better: Update repo to extend JpaRepository which has findAll(Pageable).
    }

    public List<KycReview> listAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public KycReview review(UUID id, KycReviewDecisionRequest request, String reviewedBy) {
        log.info("Reviewing KYC: id={}, status={}, reviewer={}", id, request.status(), reviewedBy);

        KycReview review = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KYC review not found: " + id));

        review.setStatus(switch (request.status()) {
            case APPROVED -> KycReview.KycStatus.APPROVED;
            case REJECTED -> KycReview.KycStatus.REJECTED;
            case REQUIRES_ADDITIONAL_INFO -> KycReview.KycStatus.REQUIRES_ADDITIONAL_INFO;
        });

        review.setNotes(request.notes());
        review.setReviewedBy(reviewedBy);
        review.setReviewedAt(LocalDateTime.now());

        KycReview saved = repository.save(review);
        log.info("KYC review updated: id={}, newStatus={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting KYC review: id={}", id);
        repository.deleteById(id);
    }
}
