package id.payu.backoffice.resource;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.*;
import id.payu.backoffice.service.CustomerCaseService;
import id.payu.backoffice.service.FraudCaseService;
import id.payu.backoffice.service.KycReviewService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Path("/api/v1/backoffice")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackofficeResource {

    private static final Logger LOG = Logger.getLogger(BackofficeResource.class);

    @Inject
    KycReviewService kycReviewService;

    @Inject
    FraudCaseService fraudCaseService;

    @Inject
    CustomerCaseService customerCaseService;

    @POST
    @Path("/kyc-reviews")
    public Response createKycReview(@Valid KycReviewRequest request) {
        LOG.infof("Creating KYC review for user: %s", request.userId());
        var review = kycReviewService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(KycReviewResponse.from(review))
                .build();
    }

    @GET
    @Path("/kyc-reviews/{id}")
    public Response getKycReview(@PathParam("id") UUID id) {
        return kycReviewService.getById(id)
                .map(review -> Response.ok(KycReviewResponse.from(review)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("KYC review not found"))
                        .build());
    }

    @GET
    @Path("/kyc-reviews")
    public List<KycReviewResponse> listKycReviews(
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        if (status != null && !status.isEmpty()) {
            var kycStatus = KycReview.KycStatus.valueOf(status.toUpperCase());
            return kycReviewService.listByStatus(kycStatus, page, size)
                    .stream()
                    .map(KycReviewResponse::from)
                    .toList();
        }
        return kycReviewService.listAll(page, size)
                .stream()
                .map(KycReviewResponse::from)
                .toList();
    }

    @POST
    @Path("/kyc-reviews/{id}/review")
    public Response reviewKyc(
            @PathParam("id") UUID id,
            @Valid KycReviewDecisionRequest request,
            @HeaderParam("X-Admin-User") String adminUser) {
        if (adminUser == null || adminUser.isEmpty()) {
            adminUser = "system";
        }
        var review = kycReviewService.review(id, request, adminUser);
        return Response.ok(KycReviewResponse.from(review)).build();
    }

    @DELETE
    @Path("/kyc-reviews/{id}")
    public Response deleteKycReview(@PathParam("id") UUID id) {
        kycReviewService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/fraud-cases")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createFraudCase(
            @FormParam("userId") String userId,
            @FormParam("accountNumber") String accountNumber,
            @FormParam("transactionId") String transactionId,
            @FormParam("transactionType") String transactionType,
            @FormParam("amount") BigDecimal amount,
            @FormParam("fraudType") String fraudType,
            @FormParam("riskLevel") String riskLevel,
            @FormParam("description") String description,
            @FormParam("evidence") String evidence) {
        var risk = riskLevel != null ? FraudCase.RiskLevel.valueOf(riskLevel.toUpperCase()) : FraudCase.RiskLevel.MEDIUM;
        var txId = transactionId != null ? UUID.fromString(transactionId) : null;
        var fraudCase = fraudCaseService.create(userId, accountNumber, txId, transactionType, 
                amount, fraudType, risk, description, evidence);
        return Response.status(Response.Status.CREATED)
                .entity(FraudCaseResponse.from(fraudCase))
                .build();
    }

    @GET
    @Path("/fraud-cases/{id}")
    public Response getFraudCase(@PathParam("id") UUID id) {
        return fraudCaseService.getById(id)
                .map(fraudCase -> Response.ok(FraudCaseResponse.from(fraudCase)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Fraud case not found"))
                        .build());
    }

    @GET
    @Path("/fraud-cases")
    public List<FraudCaseResponse> listFraudCases(
            @QueryParam("status") String status,
            @QueryParam("riskLevel") String riskLevel,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        if (riskLevel != null && !riskLevel.isEmpty()) {
            var risk = FraudCase.RiskLevel.valueOf(riskLevel.toUpperCase());
            return fraudCaseService.listByRiskLevel(risk, page, size)
                    .stream()
                    .map(FraudCaseResponse::from)
                    .toList();
        }
        if (status != null && !status.isEmpty()) {
            var caseStatus = FraudCase.CaseStatus.valueOf(status.toUpperCase());
            return fraudCaseService.listByStatus(caseStatus, page, size)
                    .stream()
                    .map(FraudCaseResponse::from)
                    .toList();
        }
        return fraudCaseService.listAll(page, size)
                .stream()
                .map(FraudCaseResponse::from)
                .toList();
    }

    @POST
    @Path("/fraud-cases/{id}/assign")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response assignFraudCase(
            @PathParam("id") UUID id,
            @FormParam("assignedTo") String assignedTo) {
        var fraudCase = fraudCaseService.assign(id, assignedTo);
        return Response.ok(FraudCaseResponse.from(fraudCase)).build();
    }

    @POST
    @Path("/fraud-cases/{id}/resolve")
    public Response resolveFraudCase(
            @PathParam("id") UUID id,
            @Valid FraudCaseDecisionRequest request,
            @HeaderParam("X-Admin-User") String adminUser) {
        if (adminUser == null || adminUser.isEmpty()) {
            adminUser = "system";
        }
        var fraudCase = fraudCaseService.resolve(id, request, adminUser);
        return Response.ok(FraudCaseResponse.from(fraudCase)).build();
    }

    @DELETE
    @Path("/fraud-cases/{id}")
    public Response deleteFraudCase(@PathParam("id") UUID id) {
        fraudCaseService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/customer-cases")
    public Response createCustomerCase(@Valid CustomerCaseRequest request) {
        var customerCase = customerCaseService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(CustomerCaseResponse.from(customerCase))
                .build();
    }

    @GET
    @Path("/customer-cases/{id}")
    public Response getCustomerCase(@PathParam("id") UUID id) {
        return customerCaseService.getById(id)
                .map(customerCase -> Response.ok(CustomerCaseResponse.from(customerCase)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Customer case not found"))
                        .build());
    }

    @GET
    @Path("/customer-cases")
    public List<CustomerCaseResponse> listCustomerCases(
            @QueryParam("status") String status,
            @QueryParam("priority") String priority,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        if (priority != null && !priority.isEmpty()) {
            var prio = CustomerCase.Priority.valueOf(priority.toUpperCase());
            return customerCaseService.listByPriority(prio, page, size)
                    .stream()
                    .map(CustomerCaseResponse::from)
                    .toList();
        }
        if (status != null && !status.isEmpty()) {
            var caseStatus = CustomerCase.CaseStatus.valueOf(status.toUpperCase());
            return customerCaseService.listByStatus(caseStatus, page, size)
                    .stream()
                    .map(CustomerCaseResponse::from)
                    .toList();
        }
        return customerCaseService.listAll(page, size)
                .stream()
                .map(CustomerCaseResponse::from)
                .toList();
    }

    @POST
    @Path("/customer-cases/{id}/assign")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response assignCustomerCase(
            @PathParam("id") UUID id,
            @FormParam("assignedTo") String assignedTo) {
        var customerCase = customerCaseService.assign(id, assignedTo);
        return Response.ok(CustomerCaseResponse.from(customerCase)).build();
    }

    @PUT
    @Path("/customer-cases/{id}")
    public Response updateCustomerCase(
            @PathParam("id") UUID id,
            @Valid CustomerCaseUpdateRequest request,
            @HeaderParam("X-Admin-User") String adminUser) {
        if (adminUser == null || adminUser.isEmpty()) {
            adminUser = "system";
        }
        var customerCase = customerCaseService.update(id, request, adminUser);
        return Response.ok(CustomerCaseResponse.from(customerCase)).build();
    }

    @DELETE
    @Path("/customer-cases/{id}")
    public Response deleteCustomerCase(@PathParam("id") UUID id) {
        customerCaseService.delete(id);
        return Response.noContent().build();
    }

    record ErrorResponse(String message) {}
}
