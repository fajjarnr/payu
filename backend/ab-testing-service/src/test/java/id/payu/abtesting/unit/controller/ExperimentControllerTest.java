package id.payu.abtesting.unit.controller;

import id.payu.abtesting.domain.entity.Experiment;
import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import id.payu.abtesting.domain.service.ExperimentService;
import id.payu.abtesting.interfaces.dto.ExperimentResponse;
import id.payu.abtesting.interfaces.rest.ExperimentController;
import id.payu.api.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExperimentController (without Spring context)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExperimentController Tests")
class ExperimentControllerTest {

    @Mock
    private ExperimentService experimentService;

    @InjectMocks
    private ExperimentController controller;

    private Experiment testExperiment;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        // Manual instantiation instead of builder to bypass Lombok issues
        testExperiment = new Experiment();
        testExperiment.setId(testId);
        testExperiment.setName("Test Experiment");
        testExperiment.setKey("test_experiment");
        testExperiment.setStatus(ExperimentStatus.RUNNING);
        testExperiment.setStartDate(LocalDate.now());
        testExperiment.setEndDate(LocalDate.now().plusDays(30));
        testExperiment.setTrafficSplit(50);
        testExperiment.setVariantAConfig(Map.of("color", "green"));
        testExperiment.setVariantBConfig(Map.of("color", "blue"));
    }

    @Test
    @DisplayName("Should get experiment by ID")
    void shouldGetExperimentById() {
        // Given
        when(experimentService.getExperimentById(testId)).thenReturn(testExperiment);

        // When
        ResponseEntity<ApiResponse<ExperimentResponse>> response = controller.getExperimentById(testId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getId()).isEqualTo(testId);
        assertThat(response.getBody().getData().getName()).isEqualTo("Test Experiment");
    }

    @Test
    @DisplayName("Should get active experiments")
    void shouldGetActiveExperiments() {
        // Given
        when(experimentService.getActiveExperiments()).thenReturn(List.of(testExperiment));

        // When
        ResponseEntity<ApiResponse<List<ExperimentResponse>>> response = controller.getActiveExperiments();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getKey()).isEqualTo("test_experiment");
    }
}
