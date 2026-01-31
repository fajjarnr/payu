package id.payu.abtesting.interfaces.rest;

import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import id.payu.abtesting.domain.service.ExperimentService;
import id.payu.abtesting.interfaces.dto.*;
import id.payu.abtesting.domain.entity.Experiment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Experiment management
 */
@RestController
@RequestMapping("/api/v1/experiments")
@RequiredArgsConstructor
@Tag(name = "Experiments", description = "A/B Testing Experiment API")
@SecurityRequirement(name = "bearerAuth")
public class ExperimentController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(ExperimentController.class);
    private final ExperimentService experimentService;

    /**
     * List all experiments with pagination
     */
    @GetMapping
    @Operation(summary = "List all experiments", description = "Get paginated list of all experiments")
    @ApiResponse(responseCode = "200", description = "Experiments retrieved successfully",
            content = @Content(schema = @Schema(implementation = ExperimentResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:read')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<Page<ExperimentResponse>>> getAllExperiments(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Experiment> experiments = experimentService.getAllExperiments(pageable);
        Page<ExperimentResponse> response = experiments.map(ExperimentResponse::fromEntity);

        return ok(response, experiments);
    }

    /**
     * Get experiment by ID
     */
    @GetMapping("/{experimentId}")
    @Operation(summary = "Get experiment by ID", description = "Retrieve detailed information about an experiment")
    @ApiResponse(responseCode = "200", description = "Experiment found",
            content = @Content(schema = @Schema(implementation = ExperimentResponse.class)))
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:read')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<ExperimentResponse>> getExperimentById(
            @Parameter(description = "Experiment ID", required = true)
            @PathVariable UUID experimentId) {

        Experiment experiment = experimentService.getExperimentById(experimentId);
        return ok(ExperimentResponse.fromEntity(experiment));
    }

    /**
     * Get experiment by key
     */
    @GetMapping("/key/{key}")
    @Operation(summary = "Get experiment by key", description = "Retrieve experiment by its unique key")
    @ApiResponse(responseCode = "200", description = "Experiment found",
            content = @Content(schema = @Schema(implementation = ExperimentResponse.class)))
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:read')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<ExperimentResponse>> getExperimentByKey(
            @Parameter(description = "Experiment key", required = true)
            @PathVariable String key) {

        Experiment experiment = experimentService.getExperimentByKey(key);
        return ok(ExperimentResponse.fromEntity(experiment));
    }

    /**
     * Get active experiments
     */
    @GetMapping("/active")
    @Operation(summary = "Get active experiments", description = "Retrieve all currently running experiments")
    @ApiResponse(responseCode = "200", description = "Active experiments retrieved successfully",
            content = @Content(schema = @Schema(implementation = ExperimentResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:read')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<java.util.List<ExperimentResponse>>> getActiveExperiments() {
        java.util.List<Experiment> experiments = experimentService.getActiveExperiments();
        java.util.List<ExperimentResponse> response = experiments.stream()
                .map(ExperimentResponse::fromEntity)
                .toList();
        return ok(response);
    }

    /**
     * Create new experiment
     */
    @PostMapping
    @Operation(summary = "Create experiment", description = "Create a new A/B testing experiment")
    @ApiResponse(responseCode = "201", description = "Experiment created successfully",
            content = @Content(schema = @Schema(implementation = ExperimentResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:write')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<ExperimentResponse>> createExperiment(
            @Valid @RequestBody ExperimentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String createdBy = jwt.getSubject();

        Experiment experiment = new Experiment();
        experiment.setName(request.getName());
        experiment.setDescription(request.getDescription());
        experiment.setKey(request.getKey());
        experiment.setStatus(request.getStatus() != null ? request.getStatus() : ExperimentStatus.DRAFT);
        experiment.setStartDate(request.getStartDate());
        experiment.setEndDate(request.getEndDate());
        experiment.setTrafficSplit(request.getTrafficSplit());
        experiment.setVariantAConfig(request.getVariantAConfig());
        experiment.setVariantBConfig(request.getVariantBConfig());
        experiment.setTargetingRules(request.getTargetingRules());

        Experiment saved = experimentService.createExperiment(experiment, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(id.payu.api.common.response.ApiResponse.success(ExperimentResponse.fromEntity(saved)));
    }

    /**
     * Update experiment
     */
    @PutMapping("/{experimentId}")
    @Operation(summary = "Update experiment", description = "Update an existing experiment")
    @ApiResponse(responseCode = "200", description = "Experiment updated successfully",
            content = @Content(schema = @Schema(implementation = ExperimentResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:write')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<ExperimentResponse>> updateExperiment(
            @Parameter(description = "Experiment ID", required = true)
            @PathVariable UUID experimentId,
            @Valid @RequestBody ExperimentRequest request) {

        Experiment updates = new Experiment();
        updates.setName(request.getName());
        updates.setDescription(request.getDescription());
        updates.setKey(request.getKey());
        updates.setStatus(request.getStatus());
        updates.setStartDate(request.getStartDate());
        updates.setEndDate(request.getEndDate());
        updates.setTrafficSplit(request.getTrafficSplit());
        updates.setVariantAConfig(request.getVariantAConfig());
        updates.setVariantBConfig(request.getVariantBConfig());
        updates.setTargetingRules(request.getTargetingRules());

        Experiment updated = experimentService.updateExperiment(experimentId, updates);
        return ok(ExperimentResponse.fromEntity(updated));
    }

    /**
     * Delete experiment
     */
    @DeleteMapping("/{experimentId}")
    @Operation(summary = "Delete experiment", description = "Delete an experiment (only if not running)")
    @ApiResponse(responseCode = "240", description = "Experiment deleted successfully")
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:delete')")
    public ResponseEntity<Void> deleteExperiment(
            @Parameter(description = "Experiment ID", required = true)
            @PathVariable UUID experimentId) {

        experimentService.deleteExperiment(experimentId);
        return noContent();
    }

    /**
     * Change experiment status
     */
    @PatchMapping("/{experimentId}/status")
    @Operation(summary = "Change experiment status", description = "Change the status of an experiment")
    @ApiResponse(responseCode = "200", description = "Experiment status updated successfully",
            content = @Content(schema = @Schema(implementation = ExperimentResponse.class)))
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:write')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<ExperimentResponse>> changeStatus(
            @Parameter(description = "Experiment ID", required = true)
            @PathVariable UUID experimentId,
            @Parameter(description = "New status", required = true)
            @RequestParam ExperimentStatus status) {

        Experiment updated = experimentService.changeStatus(experimentId, status);
        return ok(ExperimentResponse.fromEntity(updated));
    }

    /**
     * Assign variant to user
     */
    @PostMapping("/{key}/assign")
    @Operation(summary = "Assign variant to user", description = "Get variant assignment for a user (consistent hashing)")
    @ApiResponse(responseCode = "200", description = "Variant assigned successfully",
            content = @Content(schema = @Schema(implementation = VariantAssignmentResponse.class)))
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:assign')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<VariantAssignmentResponse>> assignVariant(
            @Parameter(description = "Experiment key", required = true)
            @PathVariable String key,
            @Valid @RequestBody VariantAssignmentRequest request) {

        ExperimentService.VariantAssignment assignment =
                experimentService.assignVariant(key, request.getUserId());

        return ok(VariantAssignmentResponse.fromDomain(assignment));
    }

    /**
     * Track conversion event
     */
    @PostMapping("/{experimentId}/track")
    @Operation(summary = "Track conversion", description = "Track a conversion or participation event")
    @ApiResponse(responseCode = "202", description = "Conversion tracked successfully")
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasAuthority('ab-testing:experiments:track')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<Void>> trackConversion(
            @Parameter(description = "Experiment ID", required = true)
            @PathVariable UUID experimentId,
            @Valid @RequestBody ConversionTrackingRequest request) {

        experimentService.trackConversion(experimentId, request.getUserId(), request.getVariant(), request.getEventType());
        return ResponseEntity.accepted().body(id.payu.api.common.response.ApiResponse.success(null));
    }
}
