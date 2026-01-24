package id.payu.cms.service;

import id.payu.cms.application.service.ContentService;
import id.payu.cms.domain.dto.ContentRequest;
import id.payu.cms.domain.entity.Content;
import id.payu.cms.domain.repository.ContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContentService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentService Unit Tests")
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private ContentService contentService;

    private ContentRequest contentRequest;
    private Content content;

    @BeforeEach
    void setUp() {
        contentRequest = ContentRequest.builder()
            .contentType("BANNER")
            .title("Test Banner")
            .description("Test Description")
            .imageUrl("https://example.com/image.png")
            .actionUrl("https://example.com")
            .actionType("LINK")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .priority(100)
            .targetingRules(new HashMap<>())
            .metadata(new HashMap<>())
            .build();

        content = Content.builder()
            .id(UUID.randomUUID())
            .contentType("BANNER")
            .title("Test Banner")
            .description("Test Description")
            .imageUrl("https://example.com/image.png")
            .actionUrl("https://example.com")
            .actionType("LINK")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .priority(100)
            .status(Content.ContentStatus.DRAFT)
            .targetingRules(new HashMap<>())
            .metadata(new HashMap<>())
            .version(1)
            .createdBy("admin")
            .build();
    }

    @Test
    @DisplayName("Should create content successfully")
    void shouldCreateContentSuccessfully() {
        // Given
        when(contentRepository.existsByTitleIgnoreCase("Test Banner")).thenReturn(false);
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        // When
        var response = contentService.createContent(contentRequest, "admin");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Banner");
        assertThat(response.getContentType()).isEqualTo("BANNER");
        verify(contentRepository).save(any(Content.class));
    }

    @Test
    @DisplayName("Should throw exception when creating content with duplicate title")
    void shouldThrowExceptionWhenCreatingContentWithDuplicateTitle() {
        // Given
        when(contentRepository.existsByTitleIgnoreCase("Test Banner")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> contentService.createContent(contentRequest, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    @DisplayName("Should get content by ID successfully")
    void shouldGetContentByIdSuccessfully() {
        // Given
        UUID contentId = content.getId();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        // When
        var response = contentService.getContentById(contentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(contentId);
        verify(contentRepository).findById(contentId);
    }

    @Test
    @DisplayName("Should throw exception when content not found")
    void shouldThrowExceptionWhenContentNotFound() {
        // Given
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> contentService.getContentById(contentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should delete content successfully")
    void shouldDeleteContentSuccessfully() {
        // Given
        UUID contentId = content.getId();
        when(contentRepository.existsById(contentId)).thenReturn(true);
        doNothing().when(contentRepository).deleteById(contentId);

        // When
        contentService.deleteContent(contentId);

        // Then
        verify(contentRepository).deleteById(contentId);
    }

    @Test
    @DisplayName("Should update content status successfully")
    void shouldUpdateContentStatusSuccessfully() {
        // Given
        UUID contentId = content.getId();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        // When
        var response = contentService.updateContentStatus(contentId, "ACTIVE", "admin");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(contentRepository).save(any(Content.class));
    }

    @Test
    @DisplayName("Should check if content is active")
    void shouldCheckIfContentIsActive() {
        // Given
        content.setStatus(Content.ContentStatus.ACTIVE);
        content.setStartDate(LocalDate.now().minusDays(1));
        content.setEndDate(LocalDate.now().plusDays(1));

        // When
        boolean isActive = content.isActive();

        // Then
        assertThat(isActive).isTrue();
    }

    @Test
    @DisplayName("Should return false when content status is not active")
    void shouldReturnFalseWhenContentStatusIsNotActive() {
        // Given
        content.setStatus(Content.ContentStatus.DRAFT);

        // When
        boolean isActive = content.isActive();

        // Then
        assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("Should match targeting rules")
    void shouldMatchTargetingRules() {
        // Given
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("segment", "PREMIUM");
        rules.put("location", "JAKARTA");
        content.setTargetingRules(rules);

        // When
        boolean matches = content.matchesTargeting("PREMIUM", "JAKARTA", "MOBILE");

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match targeting rules when segment differs")
    void shouldNotMatchTargetingRulesWhenSegmentDiffers() {
        // Given
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("segment", "PREMIUM");
        content.setTargetingRules(rules);

        // When
        boolean matches = content.matchesTargeting("BASIC", "JAKARTA", "MOBILE");

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should match targeting rules when rules are empty")
    void shouldMatchTargetingRulesWhenRulesAreEmpty() {
        // Given
        content.setTargetingRules(null);

        // When
        boolean matches = content.matchesTargeting("ANY", "ANY", "ANY");

        // Then
        assertThat(matches).isTrue();
    }
}
