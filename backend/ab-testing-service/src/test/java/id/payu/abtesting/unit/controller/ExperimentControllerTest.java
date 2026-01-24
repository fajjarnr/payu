package id.payu.abtesting.unit.controller;

import id.payu.abtesting.domain.entity.Experiment;
import id.payu.abtesting.domain.entity.Experiment.ExperimentStatus;
import id.payu.abtesting.domain.service.ExperimentService;
import id.payu.abtesting.interfaces.dto.ExperimentResponse;
import id.payu.abtesting.interfaces.rest.ExperimentController;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        testExperiment = Experiment.builder()
                .id(testId)
                .name("Test Experiment")
                .key("test_experiment")
                .status(ExperimentStatus.RUNNING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .trafficSplit(50)
                .variantAConfig(Map.of("color", "green"))
                .variantBConfig(Map.of("color", "blue"))
                .build();
    }

    @Test
    @DisplayName("Should get experiment by ID")
    void shouldGetExperimentById() {
        // Given
        when(experimentService.getExperimentById(testId)).thenReturn(testExperiment);

        // When
        ResponseEntity<ExperimentResponse> response = controller.getExperimentById(testId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testId);
        assertThat(response.getBody().getName()).isEqualTo("Test Experiment");
    }

    @Test
    @DisplayName("Should get active experiments")
    void shouldGetActiveExperiments() {
        // Given
        when(experimentService.getActiveExperiments()).thenReturn(List.of(testExperiment));

        // When
        ResponseEntity<List<ExperimentResponse>> response = controller.getActiveExperiments();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getKey()).isEqualTo("test_experiment");
    }
}
