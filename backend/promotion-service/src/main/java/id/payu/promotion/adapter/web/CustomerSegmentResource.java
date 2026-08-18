package id.payu.promotion.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.promotion.application.service.CustomerSegmentService;
import id.payu.promotion.interfaces.dto.CreateCustomerSegmentRequest;
import id.payu.promotion.interfaces.dto.CustomerSegmentResponse;
import id.payu.promotion.interfaces.dto.SegmentMembersResponse;
import id.payu.promotion.interfaces.dto.UpdateCustomerSegmentRequest;
import id.payu.promotion.interfaces.dto.UserSegmentsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * BE-PROMO-001: REST endpoints for customer segmentation, previously missing
 * despite entity + migration existing.
 */
@RestController
@RequestMapping("/api/v1/segments")
@RequiredArgsConstructor
@Tag(name = "Customer Segments", description = "Customer segmentation for personalized promotions")
@SecurityRequirement(name = "bearerAuth")
public class CustomerSegmentResource {

    private final CustomerSegmentService segmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING')")
    @Operation(summary = "Create a customer segment")
    public ResponseEntity<ApiResponse<CustomerSegmentResponse>> create(
            @Valid @RequestBody CreateCustomerSegmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(segmentService.create(request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all customer segments")
    public ResponseEntity<ApiResponse<List<CustomerSegmentResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(segmentService.listAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List active customer segments")
    public ResponseEntity<ApiResponse<List<CustomerSegmentResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success(segmentService.listActive()));
    }

    @GetMapping("/{segmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer segment by ID")
    public ResponseEntity<ApiResponse<CustomerSegmentResponse>> getById(
            @Parameter(description = "Segment ID", required = true) @PathVariable UUID segmentId) {
        return segmentService.getById(segmentId)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{segmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING')")
    @Operation(summary = "Update a customer segment")
    public ResponseEntity<ApiResponse<CustomerSegmentResponse>> update(
            @Parameter(description = "Segment ID", required = true) @PathVariable UUID segmentId,
            @Valid @RequestBody UpdateCustomerSegmentRequest request) {
        return segmentService.update(segmentId, request)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{segmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a customer segment")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Segment ID", required = true) @PathVariable UUID segmentId) {
        return segmentService.delete(segmentId)
                ? ResponseEntity.ok(ApiResponse.success(null))
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{accountId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get segments a user belongs to")
    public ResponseEntity<ApiResponse<UserSegmentsResponse>> getByAccount(
            @Parameter(description = "Account ID", required = true) @PathVariable String accountId) {
        return ResponseEntity.ok(ApiResponse.success(segmentService.getByAccount(accountId)));
    }

    @GetMapping("/{segmentId}/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING')")
    @Operation(summary = "Get members of a segment")
    public ResponseEntity<ApiResponse<SegmentMembersResponse>> getMembers(
            @Parameter(description = "Segment ID", required = true) @PathVariable UUID segmentId) {
        return ResponseEntity.ok(ApiResponse.success(segmentService.getMembers(segmentId)));
    }
}
