package id.payu.abtesting.unit.service;

import id.payu.abtesting.domain.entity.Experiment;
import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import id.payu.abtesting.domain.repository.ExperimentRepository;
import id.payu.abtesting.domain.service.ExperimentService;
import id.payu.abtesting.infrastructure.kafka.producer.ExperimentEventProducer;
import id.payu.abtesting.infrastructure.redis.cache.ExperimentCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExperimentService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExperimentService Tests")
class ExperimentServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentCacheService cacheService;

    @Mock
    private ExperimentEventProducer eventProducer;

    @InjectMocks
    private ExperimentService experimentService;

    private Experiment testExperiment;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testExperiment = Experiment.builder()
                .id(testId)
                .name("Test Experiment")
                .key("test_experiment")
                .status(ExperimentStatus.DRAFT)
                .trafficSplit(50)
                .variantAConfig(Map.of("color", "green"))
                .variantBConfig(Map.of("color", "blue"))
                .metrics(Map.of(
                        "CONTROL", Map.of("participants", 0, "conversions", 0),
                        "VARIANT_B", Map.of("participants", 0, "conversions", 0)
                ))
                .build();
    }

    @Test
    @DisplayName("Should get all experiments with pagination")
    void shouldGetAllExperiments() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Experiment> page = new PageImpl<>(List.of(testExperiment));
        when(experimentRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<Experiment> result = experimentService.getAllExperiments(pageable);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Experiment");
        verify(experimentRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should get experiment by ID")
    void shouldGetExperimentById() {
        // Given
        when(experimentRepository.findById(testId)).thenReturn(Optional.of(testExperiment));

        // When
        Experiment result = experimentService.getExperimentById(testId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Experiment");
        verify(experimentRepository).findById(testId);
    }

    @Test
    @DisplayName("Should throw exception when experiment not found")
    void shouldThrowExceptionWhenNotFound() {
        // Given
        when(experimentRepository.findById(testId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> experimentService.getExperimentById(testId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Experiment not found");
    }

    @Test
    @DisplayName("Should create new experiment")
    void shouldCreateExperiment() {
        // Given
        when(experimentRepository.existsByKey(anyString())).thenReturn(false);
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Experiment result = experimentService.createExperiment(testExperiment, "test-user");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ExperimentStatus.DRAFT);
        assertThat(result.getCreatedBy()).isEqualTo("test-user");
        assertThat(result.getMetrics()).isNotNull();
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    @DisplayName("Should throw exception when creating experiment with duplicate key")
    void shouldThrowExceptionOnDuplicateKey() {
        // Given
        when(experimentRepository.existsByKey(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> experimentService.createExperiment(testExperiment, "test-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key already exists");
    }

    @Test
    @DisplayName("Should assign variant to user based on consistent hashing")
    void shouldAssignVariantToUser() {
        // Given
        String experimentKey = "test_experiment";
        UUID userId = UUID.randomUUID();
        testExperiment.setStatus(ExperimentStatus.RUNNING);
        testExperiment.setStartDate(LocalDate.now());
        testExperiment.setEndDate(LocalDate.now().plusDays(30));
        when(cacheService.getVariantAssignment(anyString(), any(UUID.class))).thenReturn(null);
        when(experimentRepository.findByKey(experimentKey)).thenReturn(Optional.of(testExperiment));

        // When
        ExperimentService.VariantAssignment result = experimentService.assignVariant(experimentKey, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVariant()).isIn("CONTROL", "VARIANT_B");
        assertThat(result.getConfig()).isNotNull();
        verify(cacheService).cacheVariantAssignment(eq(experimentKey), eq(userId), any());
    }

    @Test
    @DisplayName("Should track conversion event")
    void shouldTrackConversion() {
        // Given
        UUID userId = UUID.randomUUID();
        testExperiment.setMetrics(Map.of(
                "CONTROL", Map.of("participants", 10, "conversions", 2),
                "VARIANT_B", Map.of("participants", 8, "conversions", 1)
        ));
        when(experimentRepository.findById(testId)).thenReturn(Optional.of(testExperiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> {
            Experiment exp = invocation.getArgument(0);
            return exp;
        });

        // When
        experimentService.trackConversion(testId, userId, "CONTROL", "conversion");

        // Then
        verify(experimentRepository).save(any(Experiment.class));
        verify(eventProducer).publishConversionTracked(eq(testId), eq(userId), eq("CONTROL"), eq("conversion"));
    }

    @Test
    @DisplayName("Should change experiment status")
    void shouldChangeStatus() {
        // Given
        when(experimentRepository.findById(testId)).thenReturn(Optional.of(testExperiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Experiment result = experimentService.changeStatus(testId, ExperimentStatus.RUNNING);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
        verify(experimentRepository).save(any(Experiment.class));
        verify(eventProducer).publishStatusChanged(any(Experiment.class));
    }
}
