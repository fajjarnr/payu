package id.payu.backoffice.service;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.UniversalSearchResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class UniversalSearchService {

    private static final Logger LOG = Logger.getLogger(UniversalSearchService.class);

    public UniversalSearchResponse search(String query, String entityType, int page, int size) {
        LOG.infof("Universal search: query=%s, entityType=%s, page=%d, size=%d", query, entityType, page, size);

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
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allResults.size());

        List<UniversalSearchResponse.SearchResultItem> pagedResults = fromIndex < allResults.size()
                ? allResults.subList(fromIndex, toIndex)
                : List.of();

        LOG.infof("Universal search completed: found %d results, returning %d", total, pagedResults.size());

        return new UniversalSearchResponse(query, page, size, total, pagedResults);
    }

    private List<UniversalSearchResponse.SearchResultItem> searchKycReviews(String query) {
        List<UniversalSearchResponse.SearchResultItem> results = new ArrayList<>();

        String searchTerm = "%" + query.toLowerCase() + "%";

        List<KycReview> byUserId = KycReview.<KycReview>find(
                "LOWER(userId) LIKE ?1", searchTerm).list();
        for (KycReview review : byUserId) {
            results.add(buildKycReviewItem(review, "userId"));
        }

        List<KycReview> byAccountNumber = KycReview.<KycReview>find(
                "LOWER(accountNumber) LIKE ?1", searchTerm).list();
        for (KycReview review : byAccountNumber) {
            if (!results.stream().anyMatch(r -> r.id().equals(review.id))) {
                results.add(buildKycReviewItem(review, "accountNumber"));
            }
        }

        List<KycReview> byDocumentNumber = KycReview.<KycReview>find(
                "LOWER(documentNumber) LIKE ?1", searchTerm).list();
        for (KycReview review : byDocumentNumber) {
            if (!results.stream().anyMatch(r -> r.id().equals(review.id))) {
                results.add(buildKycReviewItem(review, "documentNumber"));
            }
        }

        List<KycReview> byFullName = KycReview.<KycReview>find(
                "LOWER(fullName) LIKE ?1", searchTerm).list();
        for (KycReview review : byFullName) {
            if (!results.stream().anyMatch(r -> r.id().equals(review.id))) {
                results.add(buildKycReviewItem(review, "fullName"));
            }
        }

        return results;
    }

    private List<UniversalSearchResponse.SearchResultItem> searchFraudCases(String query) {
        List<UniversalSearchResponse.SearchResultItem> results = new ArrayList<>();

        String searchTerm = "%" + query.toLowerCase() + "%";

        List<FraudCase> byUserId = FraudCase.<FraudCase>find(
                "LOWER(userId) LIKE ?1", searchTerm).list();
        for (FraudCase fraudCase : byUserId) {
            results.add(buildFraudCaseItem(fraudCase, "userId"));
        }

        List<FraudCase> byAccountNumber = FraudCase.<FraudCase>find(
                "LOWER(accountNumber) LIKE ?1", searchTerm).list();
        for (FraudCase fraudCase : byAccountNumber) {
            if (!results.stream().anyMatch(r -> r.id().equals(fraudCase.id))) {
                results.add(buildFraudCaseItem(fraudCase, "accountNumber"));
            }
        }

        List<FraudCase> byFraudType = FraudCase.<FraudCase>find(
                "LOWER(fraudType) LIKE ?1", searchTerm).list();
        for (FraudCase fraudCase : byFraudType) {
            if (!results.stream().anyMatch(r -> r.id().equals(fraudCase.id))) {
                results.add(buildFraudCaseItem(fraudCase, "fraudType"));
            }
        }

        return results;
    }

    private List<UniversalSearchResponse.SearchResultItem> searchCustomerCases(String query) {
        List<UniversalSearchResponse.SearchResultItem> results = new ArrayList<>();

        String searchTerm = "%" + query.toLowerCase() + "%";

        List<CustomerCase> byUserId = CustomerCase.<CustomerCase>find(
                "LOWER(userId) LIKE ?1", searchTerm).list();
        for (CustomerCase customerCase : byUserId) {
            results.add(buildCustomerCaseItem(customerCase, "userId"));
        }

        List<CustomerCase> byAccountNumber = CustomerCase.<CustomerCase>find(
                "LOWER(accountNumber) LIKE ?1", searchTerm).list();
        for (CustomerCase customerCase : byAccountNumber) {
            if (!results.stream().anyMatch(r -> r.id().equals(customerCase.id))) {
                results.add(buildCustomerCaseItem(customerCase, "accountNumber"));
            }
        }

        List<CustomerCase> byCaseNumber = CustomerCase.<CustomerCase>find(
                "LOWER(caseNumber) LIKE ?1", searchTerm).list();
        for (CustomerCase customerCase : byCaseNumber) {
            if (!results.stream().anyMatch(r -> r.id().equals(customerCase.id))) {
                results.add(buildCustomerCaseItem(customerCase, "caseNumber"));
            }
        }

        List<CustomerCase> bySubject = CustomerCase.<CustomerCase>find(
                "LOWER(subject) LIKE ?1", searchTerm).list();
        for (CustomerCase customerCase : bySubject) {
            if (!results.stream().anyMatch(r -> r.id().equals(customerCase.id))) {
                results.add(buildCustomerCaseItem(customerCase, "subject"));
            }
        }

        return results;
    }

    private UniversalSearchResponse.SearchResultItem buildKycReviewItem(KycReview review, String matchedField) {
        var detailsBuilder = new java.util.HashMap<String, Object>();
        detailsBuilder.put("documentType", review.documentType);
        detailsBuilder.put("documentNumber", review.documentNumber);
        detailsBuilder.put("matchedField", matchedField);
        if (review.reviewedBy != null) {
            detailsBuilder.put("reviewedBy", review.reviewedBy);
        }
        if (review.reviewedAt != null) {
            detailsBuilder.put("reviewedAt", review.reviewedAt);
        }

        return new UniversalSearchResponse.SearchResultItem(
                "kyc",
                review.id,
                "KYC Review - " + review.fullName,
                "Document: " + review.documentNumber + " (" + review.documentType + ")",
                review.userId,
                review.accountNumber,
                review.status.name(),
                review.createdAt,
                detailsBuilder
        );
    }

    private UniversalSearchResponse.SearchResultItem buildFraudCaseItem(FraudCase fraudCase, String matchedField) {
        String title = fraudCase.fraudType != null 
                ? "Fraud Case - " + fraudCase.fraudType 
                : "Fraud Case";

        String description = fraudCase.description != null && !fraudCase.description.isEmpty()
                ? fraudCase.description
                : "Amount: " + fraudCase.amount;

        var detailsBuilder = new java.util.HashMap<String, Object>();
        if (fraudCase.transactionId != null) {
            detailsBuilder.put("transactionId", fraudCase.transactionId);
        }
        detailsBuilder.put("transactionType", fraudCase.transactionType);
        detailsBuilder.put("amount", fraudCase.amount);
        detailsBuilder.put("fraudType", fraudCase.fraudType);
        detailsBuilder.put("riskLevel", fraudCase.riskLevel.name());
        detailsBuilder.put("matchedField", matchedField);
        if (fraudCase.assignedTo != null) {
            detailsBuilder.put("assignedTo", fraudCase.assignedTo);
        }

        return new UniversalSearchResponse.SearchResultItem(
                "fraud",
                fraudCase.id,
                title,
                description,
                fraudCase.userId,
                fraudCase.accountNumber,
                fraudCase.status.name(),
                fraudCase.createdAt,
                detailsBuilder
        );
    }

    private UniversalSearchResponse.SearchResultItem buildCustomerCaseItem(CustomerCase customerCase, String matchedField) {
        var detailsBuilder = new java.util.HashMap<String, Object>();
        detailsBuilder.put("caseNumber", customerCase.caseNumber);
        detailsBuilder.put("caseType", customerCase.caseType.name());
        detailsBuilder.put("priority", customerCase.priority.name());
        detailsBuilder.put("matchedField", matchedField);
        if (customerCase.assignedTo != null) {
            detailsBuilder.put("assignedTo", customerCase.assignedTo);
        }

        return new UniversalSearchResponse.SearchResultItem(
                "customer",
                customerCase.id,
                "Customer Case - " + customerCase.caseNumber,
                customerCase.subject + " (" + customerCase.caseType.name() + ")",
                customerCase.userId,
                customerCase.accountNumber,
                customerCase.status.name(),
                customerCase.createdAt,
                detailsBuilder
        );
    }
}