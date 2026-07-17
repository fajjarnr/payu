package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.UniversalSearchResponse;
import id.payu.backoffice.dto.PiiMasking;
import id.payu.backoffice.domain.port.outbound.CustomerCaseRepositoryPort;
import id.payu.backoffice.domain.port.outbound.FraudCaseRepositoryPort;
import id.payu.backoffice.domain.port.outbound.KycReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversalSearchService {

    private final KycReviewRepositoryPort kycReviewRepository;
    private final FraudCaseRepositoryPort fraudCaseRepository;
    private final CustomerCaseRepositoryPort customerCaseRepository;

    // BUG-BE-040: Limit results per entity type to prevent OOM on large datasets
    private static final int MAX_RESULTS_PER_ENTITY = 200;

    public UniversalSearchResponse search(String query, String entityType, int page, int size) {
        log.info("Universal search: queryPresent={}, entityType={}, page={}, size={}",
                query != null && !query.isEmpty(), entityType, page, size);

        List<UniversalSearchResponse.SearchResultItem> allResults = new ArrayList<>();

        if (query != null && !query.isEmpty()) {
            if (entityType == null || entityType.equalsIgnoreCase("kyc")) {
                allResults.addAll(searchKycReviews(query));
            }

            if (entityType == null || entityType.equalsIgnoreCase("fraud")) {
                allResults.addAll(searchFraudCases(query));
            }

            if (entityType == null || entityType.equalsIgnoreCase("customer")) {
                allResults.addAll(searchCustomerCases(query));
            }
        }

        long total = allResults.size();
        // BUG-BE-040: Truncate results to prevent excessive memory usage
        if (allResults.size() > MAX_RESULTS_PER_ENTITY * 3) {
            log.warn("Search returned {} results, truncating to {}", allResults.size(), MAX_RESULTS_PER_ENTITY * 3);
            allResults = new ArrayList<>(allResults.subList(0, MAX_RESULTS_PER_ENTITY * 3));
            total = allResults.size();
        }
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allResults.size());

        List<UniversalSearchResponse.SearchResultItem> pagedResults = fromIndex < allResults.size()
                ? allResults.subList(fromIndex, toIndex)
                : List.of();

        log.info("Universal search completed: found {} results, returning {}", total, pagedResults.size());

        return new UniversalSearchResponse(query, page, size, total, pagedResults);
    }

    private List<UniversalSearchResponse.SearchResultItem> searchKycReviews(String query) {
        // Substring search over encrypted PII is intentionally disabled.
        return List.of();
    }

    private List<UniversalSearchResponse.SearchResultItem> searchFraudCases(String query) {
        List<UniversalSearchResponse.SearchResultItem> results = new ArrayList<>();

        // Search by Fraud Type
        List<FraudCase> byFraudType = fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query);
        for (FraudCase fraudCase : byFraudType) {
            if (results.stream().noneMatch(r -> r.id().equals(fraudCase.getId()))) {
                results.add(buildFraudCaseItem(fraudCase, "fraudType"));
            }
        }

        return results;
    }

    private List<UniversalSearchResponse.SearchResultItem> searchCustomerCases(String query) {
        List<UniversalSearchResponse.SearchResultItem> results = new ArrayList<>();

        // Search by Case Number
        List<CustomerCase> byCaseNumber = customerCaseRepository.findByCaseNumberContainingIgnoreCase(query);
        for (CustomerCase customerCase : byCaseNumber) {
            if (results.stream().noneMatch(r -> r.id().equals(customerCase.getId()))) {
                results.add(buildCustomerCaseItem(customerCase, "caseNumber"));
            }
        }

        // Search by Subject
        List<CustomerCase> bySubject = customerCaseRepository.findBySubjectContainingIgnoreCase(query);
        for (CustomerCase customerCase : bySubject) {
            if (results.stream().noneMatch(r -> r.id().equals(customerCase.getId()))) {
                results.add(buildCustomerCaseItem(customerCase, "subject"));
            }
        }

        return results;
    }

    private UniversalSearchResponse.SearchResultItem buildKycReviewItem(KycReview review, String matchedField) {
        var detailsBuilder = new java.util.HashMap<String, Object>();
        detailsBuilder.put("documentType", review.getDocumentType());
        detailsBuilder.put("documentNumber", PiiMasking.lastFour(review.getDocumentNumber()));
        detailsBuilder.put("matchedField", matchedField);
        if (review.getReviewedBy() != null) {
            detailsBuilder.put("reviewedBy", review.getReviewedBy());
        }
        if (review.getReviewedAt() != null) {
            detailsBuilder.put("reviewedAt", review.getReviewedAt());
        }

        return new UniversalSearchResponse.SearchResultItem(
                "kyc",
                review.getId(),
                "KYC Review - " + PiiMasking.name(review.getFullName()),
                "Document: " + PiiMasking.lastFour(review.getDocumentNumber()) + " (" + review.getDocumentType() + ")",
                review.getUserId(),
                PiiMasking.lastFour(review.getAccountNumber()),
                review.getStatus().name(),
                review.getCreatedAt(),
                detailsBuilder
        );
    }

    private UniversalSearchResponse.SearchResultItem buildFraudCaseItem(FraudCase fraudCase, String matchedField) {
        String title = fraudCase.getFraudType() != null 
                ? "Fraud Case - " + fraudCase.getFraudType() 
                : "Fraud Case";

        String description = fraudCase.getDescription() != null && !fraudCase.getDescription().isEmpty()
                ? fraudCase.getDescription()
                : "Amount: " + fraudCase.getAmount();

        var detailsBuilder = new java.util.HashMap<String, Object>();
        if (fraudCase.getTransactionId() != null) {
            detailsBuilder.put("transactionId", fraudCase.getTransactionId());
        }
        detailsBuilder.put("transactionType", fraudCase.getTransactionType());
        detailsBuilder.put("amount", fraudCase.getAmount());
        detailsBuilder.put("fraudType", fraudCase.getFraudType());
        detailsBuilder.put("riskLevel", fraudCase.getRiskLevel().name());
        detailsBuilder.put("matchedField", matchedField);
        if (fraudCase.getAssignedTo() != null) {
            detailsBuilder.put("assignedTo", fraudCase.getAssignedTo());
        }

        return new UniversalSearchResponse.SearchResultItem(
                "fraud",
                fraudCase.getId(),
                title,
                description,
                PiiMasking.lastFour(fraudCase.getUserId()),
                PiiMasking.lastFour(fraudCase.getAccountNumber()),
                fraudCase.getStatus().name(),
                fraudCase.getCreatedAt(),
                detailsBuilder
        );
    }

    private UniversalSearchResponse.SearchResultItem buildCustomerCaseItem(CustomerCase customerCase, String matchedField) {
        var detailsBuilder = new java.util.HashMap<String, Object>();
        detailsBuilder.put("caseNumber", customerCase.getCaseNumber());
        detailsBuilder.put("caseType", customerCase.getCaseType().name());
        detailsBuilder.put("priority", customerCase.getPriority().name());
        detailsBuilder.put("matchedField", matchedField);
        if (customerCase.getAssignedTo() != null) {
            detailsBuilder.put("assignedTo", customerCase.getAssignedTo());
        }

        return new UniversalSearchResponse.SearchResultItem(
                "customer",
                customerCase.getId(),
                "Customer Case - " + customerCase.getCaseNumber(),
                customerCase.getSubject() + " (" + customerCase.getCaseType().name() + ")",
                PiiMasking.lastFour(customerCase.getUserId()),
                PiiMasking.lastFour(customerCase.getAccountNumber()),
                customerCase.getStatus().name(),
                customerCase.getCreatedAt(),
                detailsBuilder
        );
    }
}
