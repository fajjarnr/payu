package id.payu.backoffice.application.service;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity;
import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import id.payu.backoffice.dto.UniversalSearchResponse;
import id.payu.backoffice.adapter.persistence.repository.CustomerCaseRepository;
import id.payu.backoffice.adapter.persistence.repository.FraudCaseRepository;
import id.payu.backoffice.adapter.persistence.repository.KycReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversalSearchService {

    private final KycReviewRepository kycReviewRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final CustomerCaseRepository customerCaseRepository;

    // BUG-BE-040: Limit results per entity type to prevent OOM on large datasets
    private static final int MAX_RESULTS_PER_ENTITY = 200;

    public UniversalSearchResponse search(String query, String entityType, int page, int size) {
        log.info("Universal search: query={}, entityType={}, page={}, size={}", query, entityType, page, size);

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
        List<UniversalSearchResponse.SearchResultItem> results = new ArrayList<>();

        // Search by User ID
        List<KycReviewEntity> byUserId = kycReviewRepository.findByUserIdContainingIgnoreCase(query);
        for (KycReviewEntity review : byUserId) {
            results.add(buildKycReviewItem(review, "userId"));
        }

        // Search by Account Number
        List<KycReviewEntity> byAccountNumber = kycReviewRepository.findByAccountNumberContainingIgnoreCase(query);
        for (KycReviewEntity review : byAccountNumber) {
            if (results.stream().noneMatch(r -> r.id().equals(review.getId()))) {
                results.add(buildKycReviewItem(review, "accountNumber"));
            }
        }

        // Search by Document Number
        List<KycReviewEntity> byDocumentNumber = kycReviewRepository.findByDocumentNumberContainingIgnoreCase(query);
        for (KycReviewEntity review : byDocumentNumber) {
            if (results.stream().noneMatch(r -> r.id().equals(review.getId()))) {
                results.add(buildKycReviewItem(review, "documentNumber"));
            }
        }

        // Search by Full Name
        List<KycReviewEntity> byFullName = kycReviewRepository.findByFullNameContainingIgnoreCase(query);
        for (KycReviewEntity review : byFullName) {
            if (results.stream().noneMatch(r -> r.id().equals(review.getId()))) {
                results.add(buildKycReviewItem(review, "fullName"));
            }
        }

        return results;
    }

    private List<UniversalSearchResponse.SearchResultItem> searchFraudCases(String query) {
        List<UniversalSearchResponse.SearchResultItem> results = new ArrayList<>();

        // Search by User ID
        List<FraudCaseEntity> byUserId = fraudCaseRepository.findByUserIdContainingIgnoreCase(query);
        for (FraudCaseEntity fraudCase : byUserId) {
            results.add(buildFraudCaseItem(fraudCase, "userId"));
        }

        // Search by Account Number
        List<FraudCaseEntity> byAccountNumber = fraudCaseRepository.findByAccountNumberContainingIgnoreCase(query);
        for (FraudCaseEntity fraudCase : byAccountNumber) {
            if (results.stream().noneMatch(r -> r.id().equals(fraudCase.getId()))) {
                results.add(buildFraudCaseItem(fraudCase, "accountNumber"));
            }
        }

        // Search by Fraud Type
        List<FraudCaseEntity> byFraudType = fraudCaseRepository.findByFraudTypeContainingIgnoreCase(query);
        for (FraudCaseEntity fraudCase : byFraudType) {
            if (results.stream().noneMatch(r -> r.id().equals(fraudCase.getId()))) {
                results.add(buildFraudCaseItem(fraudCase, "fraudType"));
            }
        }

        return results;
    }

    private List<UniversalSearchResponse.SearchResultItem> searchCustomerCases(String query) {
        List<UniversalSearchResponse.SearchResultItem> results = new ArrayList<>();

        // Search by User ID
        List<CustomerCaseEntity> byUserId = customerCaseRepository.findByUserIdContainingIgnoreCase(query);
        for (CustomerCaseEntity customerCase : byUserId) {
            results.add(buildCustomerCaseItem(customerCase, "userId"));
        }

        // Search by Account Number
        List<CustomerCaseEntity> byAccountNumber = customerCaseRepository.findByAccountNumberContainingIgnoreCase(query);
        for (CustomerCaseEntity customerCase : byAccountNumber) {
            if (results.stream().noneMatch(r -> r.id().equals(customerCase.getId()))) {
                results.add(buildCustomerCaseItem(customerCase, "accountNumber"));
            }
        }

        // Search by Case Number
        List<CustomerCaseEntity> byCaseNumber = customerCaseRepository.findByCaseNumberContainingIgnoreCase(query);
        for (CustomerCaseEntity customerCase : byCaseNumber) {
            if (results.stream().noneMatch(r -> r.id().equals(customerCase.getId()))) {
                results.add(buildCustomerCaseItem(customerCase, "caseNumber"));
            }
        }

        // Search by Subject
        List<CustomerCaseEntity> bySubject = customerCaseRepository.findBySubjectContainingIgnoreCase(query);
        for (CustomerCaseEntity customerCase : bySubject) {
            if (results.stream().noneMatch(r -> r.id().equals(customerCase.getId()))) {
                results.add(buildCustomerCaseItem(customerCase, "subject"));
            }
        }

        return results;
    }

    private UniversalSearchResponse.SearchResultItem buildKycReviewItem(KycReviewEntity review, String matchedField) {
        var detailsBuilder = new java.util.HashMap<String, Object>();
        detailsBuilder.put("documentType", review.getDocumentType());
        detailsBuilder.put("documentNumber", review.getDocumentNumber());
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
                "KYC Review - " + review.getFullName(),
                "Document: " + review.getDocumentNumber() + " (" + review.getDocumentType() + ")",
                review.getUserId(),
                review.getAccountNumber(),
                review.getStatus().name(),
                review.getCreatedAt(),
                detailsBuilder
        );
    }

    private UniversalSearchResponse.SearchResultItem buildFraudCaseItem(FraudCaseEntity fraudCase, String matchedField) {
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
                fraudCase.getUserId(),
                fraudCase.getAccountNumber(),
                fraudCase.getStatus().name(),
                fraudCase.getCreatedAt(),
                detailsBuilder
        );
    }

    private UniversalSearchResponse.SearchResultItem buildCustomerCaseItem(CustomerCaseEntity customerCase, String matchedField) {
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
                customerCase.getUserId(),
                customerCase.getAccountNumber(),
                customerCase.getStatus().name(),
                customerCase.getCreatedAt(),
                detailsBuilder
        );
    }
}