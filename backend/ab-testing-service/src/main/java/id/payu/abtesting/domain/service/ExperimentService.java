package id.payu.abtesting.domain.service;

import id.payu.abtesting.domain.entity.Experiment;
import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import id.payu.abtesting.domain.repository.ExperimentRepository;
import id.payu.abtesting.infrastructure.kafka.producer.ExperimentEventProducer;
import id.payu.abtesting.infrastructure.redis.cache.ExperimentCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain service for Experiment business logic
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentCacheService cacheService;
    private final ExperimentEventProducer eventProducer;

    /**
     * Get all experiments with pagination
     */
    public Page<Experiment> getAllExperiments(Pageable pageable) {
        log.debug("Fetching all experiments with pagination: {}", pageable);
        return experimentRepository.findAll(pageable);
    }

    /**
     * Get experiment by ID
     */
    @Cacheable(value = "experiments", key = "#id")
    public Experiment getExperimentById(UUID id) {
        log.debug("Fetching experiment by ID: {}", id);
        return experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));
    }

    /**
     * Get experiment by key
     */
    @Cacheable(value = "experimentsByKey", key = "#key")
    public Experiment getExperimentByKey(String key) {
        log.debug("Fetching experiment by key: {}", key);
        return experimentRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with key: " + key));
    }

    /**
     * Get active experiments
     */
    @Cacheable(value = "activeExperiments")
    public java.util.List<Experiment> getActiveExperiments() {
        log.debug("Fetching active experiments");
        return experimentRepository.findActiveExperiments(LocalDate.now());
    }

    /**
     * Create new experiment
     */
    @Transactional
    @CacheEvict(value = "activeExperiments", allEntries = true)
    public Experiment createExperiment(Experiment experiment, String createdBy) {
        log.info("Creating new experiment: {}", experiment.getName());

        // Validate unique key
        if (experimentRepository.existsByKey(experiment.getKey())) {
            throw new IllegalArgumentException("Experiment key already exists: " + experiment.getKey());
        }

        // Validate traffic split
        if (experiment.getTrafficSplit() < 0 || experiment.getTrafficSplit() > 100) {
            throw new IllegalArgumentException("Traffic split must be between 0 and 100");
        }

        // Set initial values
        experiment.setId(UUID.randomUUID());
        experiment.setStatus(ExperimentStatus.DRAFT);
        experiment.setCreatedBy(createdBy);
        experiment.setMetrics(Map.of(
                "CONTROL", Map.of("participants", 0, "conversions", 0),
                "VARIANT_B", Map.of("participants", 0, "conversions", 0)
        ));

        Experiment saved = experimentRepository.save(experiment);
        log.info("Created experiment with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Update existing experiment
     */
    @Transactional
    @CacheEvict(value = {"experiments", "experimentsByKey", "activeExperiments"}, allEntries = true)
    public Experiment updateExperiment(UUID id, Experiment updates) {
        log.info("Updating experiment: {}", id);

        Experiment existing = getExperimentById(id);

        // Cannot update running experiment key
        if (existing.getStatus() == ExperimentStatus.RUNNING && !existing.getKey().equals(updates.getKey())) {
            throw new IllegalArgumentException("Cannot change key of running experiment");
        }

        // Validate traffic split
        if (updates.getTrafficSplit() != null) {
            if (updates.getTrafficSplit() < 0 || updates.getTrafficSplit() > 100) {
                throw new IllegalArgumentException("Traffic split must be between 0 and 100");
            }
        }

        // Update fields
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getStartDate() != null) {
            existing.setStartDate(updates.getStartDate());
        }
        if (updates.getEndDate() != null) {
            existing.setEndDate(updates.getEndDate());
        }
        if (updates.getTrafficSplit() != null) {
            existing.setTrafficSplit(updates.getTrafficSplit());
        }
        if (updates.getVariantAConfig() != null) {
            existing.setVariantAConfig(updates.getVariantAConfig());
        }
        if (updates.getVariantBConfig() != null) {
            existing.setVariantBConfig(updates.getVariantBConfig());
        }
        if (updates.getTargetingRules() != null) {
            existing.setTargetingRules(updates.getTargetingRules());
        }

        Experiment saved = experimentRepository.save(existing);
        eventProducer.publishExperimentUpdated(saved);
        return saved;
    }

    /**
     * Delete experiment
     */
    @Transactional
    @CacheEvict(value = {"experiments", "experimentsByKey", "activeExperiments"}, allEntries = true)
    public void deleteExperiment(UUID id) {
        log.info("Deleting experiment: {}", id);

        Experiment experiment = getExperimentById(id);

        // Cannot delete running experiment
        if (experiment.getStatus() == ExperimentStatus.RUNNING) {
            throw new IllegalArgumentException("Cannot delete running experiment");
        }

        experimentRepository.deleteById(id);
        eventProducer.publishExperimentDeleted(id);
    }

    /**
     * Assign variant to user (consistent hashing)
     */
    public VariantAssignment assignVariant(String experimentKey, UUID userId) {
        log.debug("Assigning variant for experiment: {}, user: {}", experimentKey, userId);

        // Try cache first
        VariantAssignment cached = cacheService.getVariantAssignment(experimentKey, userId);
        if (cached != null) {
            return cached;
        }

        Experiment experiment = getExperimentByKey(experimentKey);

        // Check if experiment is running
        if (!experiment.isRunning()) {
            throw new IllegalArgumentException("Experiment is not currently running: " + experimentKey);
        }

        // Get variant based on consistent hashing
        String variant = experiment.getVariantForUser(userId);

        // Create assignment
        VariantAssignment assignment = VariantAssignment.builder()
                .experimentKey(experiment.getKey())
                .variant(variant)
                .config("CONTROL".equals(variant) ? experiment.getVariantAConfig() : experiment.getVariantBConfig())
                .build();

        // Cache the assignment
        cacheService.cacheVariantAssignment(experimentKey, userId, assignment);

        // Publish assignment event
        eventProducer.publishVariantAssigned(experiment.getId(), userId, variant);

        return assignment;
    }

    /**
     * Track conversion event
     */
    @Transactional
    @CacheEvict(value = {"experiments", "experimentsByKey"}, allEntries = true)
    public void trackConversion(UUID experimentId, UUID userId, String variant, String eventType) {
        log.debug("Tracking conversion for experiment: {}, user: {}, variant: {}, type: {}",
                experimentId, userId, variant, eventType);

        Experiment experiment = getExperimentById(experimentId);

        // Update metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = new HashMap<>(experiment.getMetrics());
        @SuppressWarnings("unchecked")
        Map<String, Object> variantMetrics = new HashMap<>((Map<String, Object>) metrics.get(variant));

        int participants = ((Number) variantMetrics.getOrDefault("participants", 0)).intValue();
        int conversions = ((Number) variantMetrics.getOrDefault("conversions", 0)).intValue();

        if ("conversion".equals(eventType)) {
            variantMetrics.put("conversions", conversions + 1);
        } else {
            variantMetrics.put("participants", participants + 1);
        }

        metrics.put(variant, variantMetrics);
        experiment.setMetrics(metrics);
        experimentRepository.save(experiment);

        // Publish conversion event
        eventProducer.publishConversionTracked(experimentId, userId, variant, eventType);
    }

    /**
     * Change experiment status
     */
    @Transactional
    @CacheEvict(value = {"experiments", "experimentsByKey", "activeExperiments"}, allEntries = true)
    public Experiment changeStatus(UUID id, ExperimentStatus newStatus) {
        log.info("Changing experiment status: {} -> {}", id, newStatus);

        Experiment experiment = getExperimentById(id);
        experiment.setStatus(newStatus);

        Experiment saved = experimentRepository.save(experiment);
        eventProducer.publishStatusChanged(saved);
        return saved;
    }

    /**
     * DTO for variant assignment response
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VariantAssignment {
        private String experimentKey;
        private String variant; // CONTROL or VARIANT_B
        private Map<String, Object> config;
    }
}
