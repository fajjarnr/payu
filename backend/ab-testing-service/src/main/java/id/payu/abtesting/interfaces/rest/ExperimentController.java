package id.payu.abtesting.interfaces.rest;

import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import id.payu.abtesting.domain.service.ExperimentService;
import id.payu.abtesting.interfaces.dto.*;
import id.payu.abtesting.domain.entity.Experiment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@Slf4j
@Tag(name = "Experiments", description = "A/B Testing Experiment API")
public class ExperimentController {

    private final ExperimentService experimentService;

    /**
     * List all experiments with pagination
     */
    @GetMapping
    @Operation(summary = "List all experiments", description = "Get paginated list of all experiments")
    @PreAuthorize("hasAuthority('ab-testing:experiments:read')")
    public ResponseEntity<Page<ExperimentResponse>> getAllExperiments(
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

        return ResponseEntity.ok(response);
    }

    /**
     * Get experiment by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get experiment by ID", description = "Retrieve detailed information about an experiment")
    @PreAuthorize("hasAuthority('ab-testing:experiments:read')")
    public ResponseEntity<ExperimentResponse> getExperimentById(
            @Parameter(description = "Experiment ID")
            @PathVariable UUID id) {

        Experiment experiment = experimentService.getExperimentById(id);
        return ResponseEntity.ok(ExperimentResponse.fromEntity(experiment));
    }

    /**
     * Get experiment by key
     */
    @GetMapping("/key/{key}")
    @Operation(summary = "Get experiment by key", description = "Retrieve experiment by its unique key")
    @PreAuthorize("hasAuthority('ab-testing:experiments:read')")
    public ResponseEntity<ExperimentResponse> getExperimentByKey(
            @Parameter(description = "Experiment key")
            @PathVariable String key) {

        Experiment experiment = experimentService.getExperimentByKey(key);
        return ResponseEntity.ok(ExperimentResponse.fromEntity(experiment));
    }

    /**
     * Get active experiments
     */
    @GetMapping("/active")
    @Operation(summary = "Get active experiments", description = "Retrieve all currently running experiments")
    @PreAuthorize("hasAuthority('ab-testing:experiments:read')")
    public ResponseEntity<java.util.List<ExperimentResponse>> getActiveExperiments() {
        java.util.List<Experiment> experiments = experimentService.getActiveExperiments();
        java.util.List<ExperimentResponse> response = experiments.stream()
                .map(ExperimentResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Create new experiment
     */
    @PostMapping
    @Operation(summary = "Create experiment", description = "Create a new A/B testing experiment")
    @PreAuthorize("hasAuthority('ab-testing:experiments:write')")
    public ResponseEntity<ExperimentResponse> createExperiment(
            @Valid @RequestBody ExperimentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String createdBy = jwt.getSubject();

        Experiment experiment = Experiment.builder()
                .name(request.getName())
                .description(request.getDescription())
                .key(request.getKey())
                .status(request.getStatus() != null ? request.getStatus() : ExperimentStatus.DRAFT)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .trafficSplit(request.getTrafficSplit())
                .variantAConfig(request.getVariantAConfig())
                .variantBConfig(request.getVariantBConfig())
                .targetingRules(request.getTargetingRules())
                .build();

        Experiment saved = experimentService.createExperiment(experiment, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExperimentResponse.fromEntity(saved));
    }

    /**
     * Update experiment
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update experiment", description = "Update an existing experiment")
    @PreAuthorize("hasAuthority('ab-testing:experiments:write')")
    public ResponseEntity<ExperimentResponse> updateExperiment(
            @Parameter(description = "Experiment ID")
            @PathVariable UUID id,
            @Valid @RequestBody ExperimentRequest request) {

        Experiment updates = Experiment.builder()
                .name(request.getName())
                .description(request.getDescription())
                .key(request.getKey())
                .status(request.getStatus())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .trafficSplit(request.getTrafficSplit())
                .variantAConfig(request.getVariantAConfig())
                .variantBConfig(request.getVariantBConfig())
                .targetingRules(request.getTargetingRules())
                .build();

        Experiment updated = experimentService.updateExperiment(id, updates);
        return ResponseEntity.ok(ExperimentResponse.fromEntity(updated));
    }

    /**
     * Delete experiment
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete experiment", description = "Delete an experiment (only if not running)")
    @PreAuthorize("hasAuthority('ab-testing:experiments:delete')")
    public ResponseEntity<Void> deleteExperiment(
            @Parameter(description = "Experiment ID")
            @PathVariable UUID id) {

        experimentService.deleteExperiment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change experiment status
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change experiment status", description = "Change the status of an experiment")
    @PreAuthorize("hasAuthority('ab-testing:experiments:write')")
    public ResponseEntity<ExperimentResponse> changeStatus(
            @Parameter(description = "Experiment ID")
            @PathVariable UUID id,
            @Parameter(description = "New status")
            @RequestParam ExperimentStatus status) {

        Experiment updated = experimentService.changeStatus(id, status);
        return ResponseEntity.ok(ExperimentResponse.fromEntity(updated));
    }

    /**
     * Assign variant to user
     */
    @PostMapping("/{key}/assign")
    @Operation(summary = "Assign variant to user", description = "Get variant assignment for a user (consistent hashing)")
    @PreAuthorize("hasAuthority('ab-testing:experiments:assign')")
    public ResponseEntity<VariantAssignmentResponse> assignVariant(
            @Parameter(description = "Experiment key")
            @PathVariable String key,
            @Valid @RequestBody VariantAssignmentRequest request) {

        ExperimentService.VariantAssignment assignment =
                experimentService.assignVariant(key, request.getUserId());

        return ResponseEntity.ok(VariantAssignmentResponse.fromDomain(assignment));
    }

    /**
     * Track conversion event
     */
    @PostMapping("/{id}/track")
    @Operation(summary = "Track conversion", description = "Track a conversion or participation event")
    @PreAuthorize("hasAuthority('ab-testing:experiments:track')")
    public ResponseEntity<Void> trackConversion(
            @Parameter(description = "Experiment ID")
            @PathVariable UUID id,
            @Valid @RequestBody ConversionTrackingRequest request) {

        experimentService.trackConversion(id, request.getUserId(), request.getVariant(), request.getEventType());
        return ResponseEntity.accepted().build();
    }
}
