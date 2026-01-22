package id.payu.backoffice.service;

import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.KycReviewDecisionRequest;
import id.payu.backoffice.dto.KycReviewRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class KycReviewService {

    private static final Logger LOG = Logger.getLogger(KycReviewService.class);

    @Transactional
    public KycReview create(KycReviewRequest request) {
        LOG.infof("Creating KYC review for user: %s", request.userId());

        KycReview review = new KycReview();
        review.userId = request.userId();
        review.accountNumber = request.accountNumber();
        review.documentType = request.documentType();
        review.documentNumber = request.documentNumber();
        review.documentUrl = request.documentUrl();
        review.fullName = request.fullName();
        review.address = request.address();
        review.phoneNumber = request.phoneNumber();
        review.notes = request.notes();
        review.status = KycReview.KycStatus.PENDING;

        review.persist();
        LOG.infof("KYC review created: id=%s", review.id);
        return review;
    }

    public Optional<KycReview> getById(UUID id) {
        return KycReview.findByIdOptional(id);
    }

    public Optional<KycReview> getByUserId(String userId) {
        return KycReview.<KycReview>find("userId = ?1 ORDER BY createdAt DESC", userId)
                .firstResultOptional();
    }

    public List<KycReview> listByStatus(KycReview.KycStatus status, int page, int size) {
        return KycReview.<KycReview>find("status = ?1 ORDER BY createdAt DESC", status)
                .page(page, size)
                .list();
    }

    public List<KycReview> listAll(int page, int size) {
        return KycReview.<KycReview>findAll()
                .page(page, size)
                .list();
    }

    @Transactional
    public KycReview review(UUID id, KycReviewDecisionRequest request, String reviewedBy) {
        LOG.infof("Reviewing KYC: id=%s, status=%s, reviewer=%s", id, request.status(), reviewedBy);

        KycReview review = KycReview.<KycReview>findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("KYC review not found: " + id));

        review.status = switch (request.status()) {
            case APPROVED -> KycReview.KycStatus.APPROVED;
            case REJECTED -> KycReview.KycStatus.REJECTED;
            case REQUIRES_ADDITIONAL_INFO -> KycReview.KycStatus.REQUIRES_ADDITIONAL_INFO;
        };

        review.notes = request.notes();
        review.reviewedBy = reviewedBy;
        review.reviewedAt = LocalDateTime.now();

        review.persist();
        LOG.infof("KYC review updated: id=%s, newStatus=%s", review.id, review.status);
        return review;
    }

    @Transactional
    public void delete(UUID id) {
        LOG.infof("Deleting KYC review: id=%s", id);
        KycReview.deleteById(id);
    }
}
