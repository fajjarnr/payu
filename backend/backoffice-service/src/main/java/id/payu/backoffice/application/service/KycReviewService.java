package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.KycReviewDecisionRequest;
import id.payu.backoffice.dto.KycReviewRequest;
import id.payu.backoffice.domain.port.outbound.KycReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.KycStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycReviewService {

    private final KycReviewRepositoryPort repository;

    @Transactional
    public KycReview create(KycReviewRequest request) {
        log.info("Creating KYC review");
        KycReview saved = repository.save(KycReview.create(request.userId(), request.accountNumber(), request.documentType(), request.documentNumber(), request.documentUrl(), request.fullName(), request.address(), request.phoneNumber(), request.notes()));
        log.info("KYC review created: id={}", saved.getId());
        return saved;
    }

    public Optional<KycReview> getById(UUID id) {
        return repository.findById(id);
    }

    public Optional<KycReview> getByUserId(String userId) {
        return repository.findLatestByUserId(userId);
    }

    public List<KycReview> listByStatus(KycStatus status, int page, int size) {
        return repository.findByStatus(status, page, size);
    }

    public List<KycReview> listAll(int page, int size) {
        return repository.findAll(page, size);
    }

    @Transactional
    public KycReview review(UUID id, KycReviewDecisionRequest request, String reviewedBy) {
        log.info("Reviewing KYC: id={}, status={}", id, request.status());

        KycReview review = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KYC review not found: " + id));

        KycStatus status = switch (request.status()) {
            case APPROVED -> KycStatus.APPROVED;
            case REJECTED -> KycStatus.REJECTED;
            case REQUIRES_ADDITIONAL_INFO -> KycStatus.REQUIRES_ADDITIONAL_INFO;
        };
        review.review(status, request.notes(), reviewedBy);
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
