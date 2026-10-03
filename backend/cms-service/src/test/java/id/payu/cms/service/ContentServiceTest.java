package id.payu.cms.service;

import id.payu.cms.application.service.ContentService;
import id.payu.cms.interfaces.dto.ContentRequest;
import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.cms.domain.entity.ContentStatus;
import id.payu.cms.domain.port.out.ContentPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;

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
    private ContentPersistencePort contentRepository;

    @InjectMocks
    private ContentService contentService;

    private ContentRequest contentRequest;
    private ContentEntity content;

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

        content = ContentEntity.builder()
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
            .status(ContentStatus.DRAFT)
            .targetingRules(new HashMap<>())
            .metadata(new HashMap<>())
            .version(1L)
            .createdBy("admin")
            .build();
    }

    @Test
    @DisplayName("Should create content successfully")
    void shouldCreateContentSuccessfully() {
        when(contentRepository.existsByTitleIgnoreCase("Test Banner")).thenReturn(false);
        when(contentRepository.save(any(ContentEntity.class))).thenReturn(content);

        var response = contentService.createContent(contentRequest, "admin");

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Banner");
        assertThat(response.getContentType()).isEqualTo("BANNER");
        verify(contentRepository).save(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when creating content with duplicate title")
    void shouldThrowExceptionWhenCreatingContentWithDuplicateTitle() {
        when(contentRepository.existsByTitleIgnoreCase("Test Banner")).thenReturn(true);

        assertThatThrownBy(() -> contentService.createContent(contentRequest, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(contentRepository, never()).save(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should get content by ID successfully")
    void shouldGetContentByIdSuccessfully() {
        UUID contentId = content.getId();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        var response = contentService.getContentById(contentId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(contentId);
        verify(contentRepository).findById(contentId);
    }

    @Test
    @DisplayName("Should throw exception when content not found")
    void shouldThrowExceptionWhenContentNotFound() {
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.getContentById(contentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should delete content successfully")
    void shouldDeleteContentSuccessfully() {
        UUID contentId = content.getId();
        when(contentRepository.existsById(contentId)).thenReturn(true);
        doNothing().when(contentRepository).deleteById(contentId);

        contentService.deleteContent(contentId);

        verify(contentRepository).deleteById(contentId);
    }

    @Test
    @DisplayName("Should update content status successfully")
    void shouldUpdateContentStatusSuccessfully() {
        UUID contentId = content.getId();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(ContentEntity.class))).thenReturn(content);

        var response = contentService.updateContentStatus(contentId, "ACTIVE", "admin");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(contentRepository).save(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should check if content is active")
    void shouldCheckIfContentIsActive() {
        content.setStatus(ContentStatus.ACTIVE);
        content.setStartDate(LocalDate.now().minusDays(1));
        content.setEndDate(LocalDate.now().plusDays(1));

        boolean isActive = content.isActive();

        assertThat(isActive).isTrue();
    }

    @Test
    @DisplayName("Should return false when content status is not active")
    void shouldReturnFalseWhenContentStatusIsNotActive() {
        content.setStatus(ContentStatus.DRAFT);

        boolean isActive = content.isActive();

        assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("Should match targeting rules")
    void shouldMatchTargetingRules() {
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("segment", "PREMIUM");
        rules.put("location", "JAKARTA");
        content.setTargetingRules(rules);

        boolean matches = content.matchesTargeting("PREMIUM", "JAKARTA", "MOBILE");

        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match targeting rules when segment differs")
    void shouldNotMatchTargetingRulesWhenSegmentDiffers() {
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("segment", "PREMIUM");
        content.setTargetingRules(rules);

        boolean matches = content.matchesTargeting("BASIC", "JAKARTA", "MOBILE");

        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should match targeting rules when rules are empty")
    void shouldMatchTargetingRulesWhenRulesAreEmpty() {
        content.setTargetingRules(null);

        boolean matches = content.matchesTargeting("ANY", "ANY", "ANY");

        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("BUG-CMS-NPE-002: should not throw NPE when targeting rule value is null")
    void shouldNotThrowNpeWhenTargetingRuleValueIsNull() {
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("segment", null);
        content.setTargetingRules(rules);

        boolean matches = content.matchesTargeting("PREMIUM", "JAKARTA", "MOBILE");
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("BUG-CMS-NPE-002: should not throw NPE when user input is null")
    void shouldNotThrowNpeWhenUserInputIsNull() {
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("segment", "PREMIUM");
        rules.put("location", "JAKARTA");
        rules.put("device", "MOBILE");
        content.setTargetingRules(rules);

        boolean matches = content.matchesTargeting(null, null, null);
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("BUG-CMS-NPE-002: should handle mixed null rule values and null inputs")
    void shouldHandleMixedNullValues() {
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("segment", null);
        rules.put("location", "JAKARTA");
        content.setTargetingRules(rules);

        boolean matches = content.matchesTargeting("PREMIUM", "BANDUNG", "MOBILE");

        // Null rule value is treated as wildcard; location mismatch fails the match
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should update content successfully")
    void shouldUpdateContentSuccessfully() {
        UUID contentId = content.getId();
        ContentRequest updateRequest = ContentRequest.builder()
            .contentType("PROMO")
            .title("Updated Banner")
            .description("Updated Description")
            .imageUrl("https://example.com/updated.png")
            .actionUrl("https://example.com/updated")
            .actionType("LINK")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(60))
            .priority(200)
            .targetingRules(new HashMap<>())
            .metadata(new HashMap<>())
            .build();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.existsByTitleIgnoreCase("Updated Banner")).thenReturn(false);
        when(contentRepository.save(any(ContentEntity.class))).thenReturn(content);

        var response = contentService.updateContent(contentId, updateRequest, "admin");

        assertThat(response).isNotNull();
        verify(contentRepository).save(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should get content by type")
    void shouldGetContentByType() {
        when(contentRepository.findByContentType("BANNER")).thenReturn(List.of(content));

        var result = contentService.getContentByType("BANNER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContentType()).isEqualTo("BANNER");
    }

    @Test
    @DisplayName("Should get content by status")
    void shouldGetContentByStatus() {
        when(contentRepository.findByStatus(ContentStatus.DRAFT)).thenReturn(List.of(content));

        var result = contentService.getContentByStatus("draft");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Should get active content by type")
    void shouldGetActiveContentByType() {
        when(contentRepository.findActiveByContentType("BANNER", LocalDate.now())).thenReturn(List.of(content));

        var result = contentService.getActiveContentByType("BANNER");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should get scheduled content to activate")
    void shouldGetScheduledContentToActivate() {
        when(contentRepository.findScheduledToActivate(LocalDate.now())).thenReturn(List.of(content));

        var result = contentService.getScheduledContentToActivate();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should get expired active content")
    void shouldGetExpiredActiveContent() {
        when(contentRepository.findActiveToArchive(LocalDate.now())).thenReturn(List.of(content));

        var result = contentService.getExpiredActiveContent();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should activate scheduled content")
    void shouldActivateScheduledContent() {
        UUID contentId = content.getId();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        contentService.activateScheduledContent(List.of(contentId));

        assertThat(content.getStatus()).isEqualTo(ContentStatus.ACTIVE);
        verify(contentRepository).save(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should archive expired content")
    void shouldArchiveExpiredContent() {
        content.setStatus(ContentStatus.ACTIVE);
        UUID contentId = content.getId();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        contentService.archiveExpiredContent(List.of(contentId));

        assertThat(content.getStatus()).isEqualTo(ContentStatus.ARCHIVED);
        verify(contentRepository).save(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent content")
    void shouldThrowExceptionWhenDeletingNonExistentContent() {
        UUID contentId = UUID.randomUUID();
        when(contentRepository.existsById(contentId)).thenReturn(false);

        assertThatThrownBy(() -> contentService.deleteContent(contentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }


    @Test
    @DisplayName("Should get all content with pagination")
    void shouldGetAllContentWithPagination() {
        // BUG-CMS-HEX-001: port signature is findAll(int,int,String,String)
        when(contentRepository.findAll(0, 20, "createdAt", "desc"))
            .thenReturn(List.of(content));

        var result = contentService.getAllContent(0, 20, "createdAt", "desc");

        assertThat(result).isNotNull();
        assertThat(result.getContents()).hasSize(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should get all content with ascending sort")
    void shouldGetAllContentWithAscendingSort() {
        // BUG-CMS-HEX-001: port signature is findAll(int,int,String,String)
        when(contentRepository.findAll(0, 10, "title", "asc"))
            .thenReturn(List.of(content));

        var result = contentService.getAllContent(0, 10, "title", "asc");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException on concurrent create")
    void shouldHandleDataIntegrityViolationExceptionOnConcurrentCreate() {
        when(contentRepository.existsByTitleIgnoreCase("Test Banner")).thenReturn(false);
        when(contentRepository.save(any(ContentEntity.class)))
            .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThatThrownBy(() -> contentService.createContent(contentRequest, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should increment version on update")
    void shouldIncrementVersionOnUpdate() {
        UUID contentId = content.getId();
        ContentRequest updateRequest = ContentRequest.builder()
            .contentType("BANNER")
            .title("Test Banner") // Same title — no uniqueness check needed
            .description("Updated Description")
            .priority(100)
            .targetingRules(new HashMap<>())
            .metadata(new HashMap<>())
            .build();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(ContentEntity.class))).thenReturn(content);

        contentService.updateContent(contentId, updateRequest, "admin");

        assertThat(content.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle null priority by defaulting to 0")
    void shouldHandleNullPriority() {
        ContentRequest requestWithNullPriority = ContentRequest.builder()
            .contentType("BANNER")
            .title("No Priority Banner")
            .description("Test")
            .priority(null)
            .build();

        when(contentRepository.existsByTitleIgnoreCase("No Priority Banner")).thenReturn(false);
        when(contentRepository.save(any(ContentEntity.class))).thenAnswer(inv -> {
            ContentEntity c = inv.getArgument(0);
            assertThat(c.getPriority()).isEqualTo(0);
            return c;
        });

        var response = contentService.createContent(requestWithNullPriority, "admin");

        assertThat(response).isNotNull();
        verify(contentRepository).save(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should return empty list when no content of requested type exists")
    void shouldReturnEmptyListWhenNoContentOfRequestedType() {
        when(contentRepository.findByContentType("POPUP")).thenReturn(Collections.emptyList());

        var result = contentService.getContentByType("POPUP");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no active content of type exists")
    void shouldReturnEmptyListWhenNoActiveContentOfType() {
        when(contentRepository.findActiveByContentType("POPUP", LocalDate.now()))
            .thenReturn(Collections.emptyList());

        var result = contentService.getActiveContentByType("POPUP");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid status value")
    void shouldThrowExceptionForInvalidStatusValue() {
        assertThatThrownBy(() -> contentService.getContentByStatus("INVALID_STATUS"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should accept case-insensitive status values")
    void shouldAcceptCaseInsensitiveStatusValues() {
        when(contentRepository.findByStatus(ContentStatus.DRAFT))
            .thenReturn(List.of(content));

        var result = contentService.getContentByStatus("dRaFt");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent content")
    void shouldThrowExceptionWhenUpdatingNonExistentContent() {
        UUID nonExistentId = UUID.randomUUID();
        when(contentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.updateContent(nonExistentId, contentRequest, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should throw exception when updating content status for non-existent content")
    void shouldThrowExceptionWhenUpdatingStatusForNonExistentContent() {
        UUID nonExistentId = UUID.randomUUID();
        when(contentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.updateContentStatus(nonExistentId, "ACTIVE", "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should reject update when title changed to existing title")
    void shouldRejectUpdateWhenTitleChangedToExistingTitle() {
        UUID contentId = content.getId();
        ContentRequest updateRequest = ContentRequest.builder()
            .contentType("BANNER")
            .title("Different Title") // Different from current "Test Banner"
            .description("Updated")
            .priority(100)
            .build();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.existsByTitleIgnoreCase("Different Title")).thenReturn(true);

        assertThatThrownBy(() -> contentService.updateContent(contentId, updateRequest, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should update content when title unchanged (skip uniqueness check)")
    void shouldUpdateContentWhenTitleUnchanged() {
        UUID contentId = content.getId();
        ContentRequest sameTitleRequest = ContentRequest.builder()
            .contentType("PROMO")
            .title("Test Banner") // Same title
            .description("Updated Description")
            .priority(200)
            .build();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        // Should NOT call existsByTitleIgnoreCase when title is unchanged
        when(contentRepository.save(any(ContentEntity.class))).thenReturn(content);

        var response = contentService.updateContent(contentId, sameTitleRequest, "admin");

        assertThat(response).isNotNull();
        verify(contentRepository, never()).existsByTitleIgnoreCase(any());
    }

    @Test
    @DisplayName("Should handle empty list for activate scheduled content")
    void shouldHandleEmptyListForActivateScheduledContent() {

        contentService.activateScheduledContent(Collections.emptyList());

        verify(contentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should handle empty list for archive expired content")
    void shouldHandleEmptyListForArchiveExpiredContent() {

        contentService.archiveExpiredContent(Collections.emptyList());

        verify(contentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should return false for isActive when dates are not within range")
    void shouldReturnFalseForIsActiveWhenDatesNotWithinRange() {
        content.setStatus(ContentStatus.ACTIVE);
        content.setStartDate(LocalDate.now().plusDays(1));
        content.setEndDate(LocalDate.now().plusDays(10));

        boolean isActive = content.isActive();

        assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("Should return false for isActive when past end date")
    void shouldReturnFalseForIsActiveWhenPastEndDate() {
        content.setStatus(ContentStatus.ACTIVE);
        content.setStartDate(LocalDate.now().minusDays(10));
        content.setEndDate(LocalDate.now().minusDays(1));

        boolean isActive = content.isActive();

        assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("Should return true for isActive with null dates (indefinite)")
    void shouldReturnTrueForIsActiveWithNullDates() {
        content.setStatus(ContentStatus.ACTIVE);
        content.setStartDate(null);
        content.setEndDate(null);

        boolean isActive = content.isActive();

        assertThat(isActive).isTrue();
    }

    @Test
    @DisplayName("Should not match targeting when location differs")
    void shouldNotMatchTargetingWhenLocationDiffers() {
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("location", "JAKARTA");
        content.setTargetingRules(rules);

        boolean matches = content.matchesTargeting("PREMIUM", "BANDUNG", "MOBILE");

        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should not match targeting when device differs")
    void shouldNotMatchTargetingWhenDeviceDiffers() {
        HashMap<String, Object> rules = new HashMap<>();
        rules.put("device", "MOBILE");
        content.setTargetingRules(rules);

        boolean matches = content.matchesTargeting("PREMIUM", "JAKARTA", "DESKTOP");

        assertThat(matches).isFalse();
    }
}
