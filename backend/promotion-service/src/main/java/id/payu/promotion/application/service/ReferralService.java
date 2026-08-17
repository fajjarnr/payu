package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.Referral;
import id.payu.promotion.interfaces.dto.CreateReferralRequest;
import id.payu.promotion.interfaces.dto.CompleteReferralRequest;
import id.payu.promotion.interfaces.dto.ReferralSummaryResponse;
import id.payu.promotion.domain.port.out.ReferralRepositoryPort;
import id.payu.promotion.domain.port.out.ReferralRewardPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import id.payu.promotion.domain.port.out.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.ReferralStatus;
import id.payu.promotion.domain.RewardStatus;
import id.payu.promotion.domain.RewardType;
import id.payu.promotion.domain.TransactionType;

@Service
public class ReferralService {

    private static final Logger LOG = LoggerFactory.getLogger(ReferralService.class);
    private static final int MONEY_SCALE = 4;

    private final ReferralRepositoryPort referralRepository;
    private final ReferralRewardPort referralRewardPort;
    private final DomainEventPublisher outboxService;
    private final String promotionEventsTopic;

    public ReferralService(
            ReferralRepositoryPort referralRepository,
            ReferralRewardPort referralRewardPort,
            DomainEventPublisher outboxService,
            @Value("${app.kafka.topics.promotion-events:payu.promotion.referral-event.v1}") String promotionEventsTopic) {
        this.referralRepository = referralRepository;
        this.referralRewardPort = referralRewardPort;
        this.outboxService = outboxService;
        this.promotionEventsTopic = promotionEventsTopic;
    }

    @Transactional
    public Referral createReferral(CreateReferralRequest request) {
        LOG.info("Creating referral: referrer={}", request.referrerAccountId());

        if (request.referrerAccountId() == null || request.referrerAccountId().isBlank()) {
            throw new IllegalArgumentException("Referrer account ID is required");
        }

        String referralCode = generateReferralCode();

        Referral referral = new Referral();
        referral.setReferrerAccountId(request.referrerAccountId());
        referral.setReferralCode(referralCode);
        referral.setReferrerReward(normalizeMoney(request.referrerReward()));
        referral.setRefereeReward(normalizeMoney(request.refereeReward()));
        referral.setRewardType(request.rewardType());
        referral.setExpiryDate(request.expiryDate());
        referral.setStatus(ReferralStatus.PENDING);

        referral = referralRepository.save(referral);

        publishReferralEvent(referral, "CREATED");

        LOG.info("ReferralEntity created: id={}, code={}", referral.getId(), referralCode);

        return referral;
    }

    @Transactional
    public Referral completeReferral(CompleteReferralRequest request) {
        // REFERRAL-001 (CB-030): pessimistic lock so concurrent completions of the
        // same code serialize; the second one sees status != PENDING and fails.
        Referral referral = referralRepository.findByReferralCodeForUpdate(request.referralCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid referral code"));

        if (referral.getStatus() != ReferralStatus.PENDING) {
            throw new IllegalArgumentException("ReferralEntity already completed or expired");
        }

        if (referral.getExpiryDate() != null && LocalDateTime.now().isAfter(referral.getExpiryDate())) {
            referral.setStatus(ReferralStatus.EXPIRED);
            referralRepository.save(referral);
            throw new IllegalArgumentException("ReferralEntity code has expired");
        }

        referral.setRefereeAccountId(request.refereeAccountId());
        referral.setStatus(ReferralStatus.COMPLETED);
        referral.setCompletedAt(LocalDateTime.now());
        referral = referralRepository.save(referral);

        grantReferralRewards(referral);

        publishReferralEvent(referral, "COMPLETED");

        LOG.info("ReferralEntity completed: code={}, referrer={}, referee={}",
            request.referralCode(), referral.getReferrerAccountId(), request.refereeAccountId());

        return referral;
    }

    public Optional<Referral> getReferral(UUID id) {
        return referralRepository.findById(id);
    }

    public Optional<Referral> getReferralByCode(String code) {
        return referralRepository.findByReferralCode(code);
    }

    public List<Referral> getReferralsByReferrer(String referrerAccountId) {
        return referralRepository.findByReferrerAccountId(referrerAccountId);
    }

    public ReferralSummaryResponse getReferralSummary(String referrerAccountId) {
        List<Referral> referrals = referralRepository.findByReferrerAccountId(referrerAccountId);
        long totalReferrals = referrals.size();
        long completedReferrals = referrals.stream()
            .filter(r -> r.getStatus() == ReferralStatus.COMPLETED)
            .count();
        long pendingReferrals = referrals.stream()
            .filter(r -> r.getStatus() == ReferralStatus.PENDING)
            .count();

        Optional<Referral> lastReferral = referrals.stream()
            .sorted(java.util.Comparator.comparing(Referral::getCreatedAt).reversed())
            .findFirst();

        String referralCode = lastReferral
            .map(Referral::getReferralCode)
            .orElse(null);

        // PROD-046: earnings = sum of completed referrer rewards, DECIMAL(19,4) HALF_EVEN
        BigDecimal totalEarnings = referrals.stream()
            .filter(r -> r.getStatus() == ReferralStatus.COMPLETED)
            .map(Referral::getReferrerReward)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, java.math.RoundingMode.HALF_EVEN);

        return new ReferralSummaryResponse(
            referralCode,
            (int) totalReferrals,
            (int) completedReferrals,
            (int) pendingReferrals,
            totalEarnings
        );
    }

    private void grantReferralRewards(Referral referral) {
        if (referral.getRewardType() == ReferralRewardType.CASHBACK) {
            grantCashbackReward(referral.getReferrerAccountId(), referral.getReferrerReward(),
                referral.getReferralCode(), "REFERRER");
            grantCashbackReward(referral.getRefereeAccountId(), referral.getRefereeReward(),
                referral.getReferralCode(), "REFEREE");
        } else if (referral.getRewardType() == ReferralRewardType.POINTS) {
            grantLoyaltyPoints(referral.getReferrerAccountId(), referral.getReferrerReward().intValue(),
                referral.getReferralCode(), TransactionType.REFERRAL_BONUS);
            grantLoyaltyPoints(referral.getRefereeAccountId(), referral.getRefereeReward().intValue(),
                referral.getReferralCode(), TransactionType.REFERRAL_BONUS);
        }
    }

    private void grantCashbackReward(String accountId, BigDecimal amount,
        String transactionId, String rewardType) {
        referralRewardPort.grantCashback(accountId, amount, transactionId);
    }

    private void grantLoyaltyPoints(String accountId, Integer points,
        String transactionId, TransactionType type) {
        referralRewardPort.grantPoints(accountId, points, transactionId, type);
    }

    private static BigDecimal normalizeMoney(BigDecimal amount) {
        return amount == null ? null : amount.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private String generateReferralCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }

    private void publishReferralEvent(Referral referral, String eventType) {
        outboxService.createEvent(
                "Referral",
                referral.getId().toString(),
                eventType,
                Map.of(
                        "referralId", referral.getId().toString(),
                        "referralCode", referral.getReferralCode(),
                        "status", referral.getStatus().name(),
                        "eventType", eventType,
                        "timestamp", LocalDateTime.now().toString()
                ),
                null,
                promotionEventsTopic
        );
    }
}
