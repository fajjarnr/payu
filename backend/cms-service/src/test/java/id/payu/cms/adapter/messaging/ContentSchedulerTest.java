package id.payu.cms.adapter.messaging;

import id.payu.cms.application.service.ContentService;
import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.cms.domain.entity.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContentScheduler.
 *
 * Validates scheduled activation and archival of content items
 * with proper event publishing.
 *
 * @author PayU QA Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentScheduler Unit Tests")
class ContentSchedulerTest {

    @Mock
    private ContentService contentService;

    @Mock
    private ContentEventPublisher eventPublisher;

    @InjectMocks
    private ContentScheduler scheduler;

    private ContentEntity scheduledContent;
    private ContentEntity expiredContent;
    private UUID scheduledId;
    private UUID expiredId;

    @BeforeEach
    void setUp() {
        scheduledId = UUID.randomUUID();
        expiredId = UUID.randomUUID();

        Map<String, Object> targeting = new HashMap<>();
        targeting.put("segment", "ALL");

        scheduledContent = ContentEntity.builder()
                .id(scheduledId)
                .contentType("BANNER")
                .title("Scheduled Banner")
                .description("To be activated")
                .actionType("LINK")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .priority(100)
                .status(ContentStatus.SCHEDULED)
                .targetingRules(targeting)
                .metadata(new HashMap<>())
                .version(1)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        expiredContent = ContentEntity.builder()
                .id(expiredId)
                .contentType("PROMO")
                .title("Expired Promo")
                .description("Should be archived")
                .actionType("LINK")
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(1))
                .priority(50)
                .status(ContentStatus.ACTIVE)
                .targetingRules(targeting)
                .metadata(new HashMap<>())
                .version(1)
                .createdBy("admin")
                .updatedBy("admin")
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Activate Scheduled ContentEntity Tests
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should activate scheduled content and publish events")
    void shouldActivateScheduledContentAndPublishEvents() {
        // Given
        when(contentService.getScheduledContentToActivate())
                .thenReturn(List.of(scheduledContent));
        doNothing().when(contentService).activateScheduledContent(anyList());

        // When
        scheduler.activateScheduledContent();

        // Then
        verify(contentService).getScheduledContentToActivate();
        verify(contentService).activateScheduledContent(List.of(scheduledId));
        verify(eventPublisher).publishContentPublished(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should not activate when no scheduled content exists")
    void shouldNotActivateWhenNoScheduledContentExists() {
        // Given
        when(contentService.getScheduledContentToActivate())
                .thenReturn(Collections.emptyList());

        // When
        scheduler.activateScheduledContent();

        // Then
        verify(contentService).getScheduledContentToActivate();
        verify(contentService, never()).activateScheduledContent(anyList());
        verify(eventPublisher, never()).publishContentPublished(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should activate multiple scheduled items")
    void shouldActivateMultipleScheduledItems() {
        // Given
        UUID secondId = UUID.randomUUID();
        ContentEntity secondScheduled = ContentEntity.builder()
                .id(secondId)
                .contentType("BANNER")
                .title("Second Scheduled Banner")
                .actionType("LINK")
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().plusDays(15))
                .priority(80)
                .status(ContentStatus.SCHEDULED)
                .version(1)
                .build();

        when(contentService.getScheduledContentToActivate())
                .thenReturn(List.of(scheduledContent, secondScheduled));

        // When
        scheduler.activateScheduledContent();

        // Then
        verify(contentService).activateScheduledContent(List.of(scheduledId, secondId));
        verify(eventPublisher, times(2)).publishContentPublished(any(ContentEntity.class));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Archive Expired ContentEntity Tests
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should archive expired content and publish events")
    void shouldArchiveExpiredContentAndPublishEvents() {
        // Given
        when(contentService.getExpiredActiveContent())
                .thenReturn(List.of(expiredContent));
        doNothing().when(contentService).archiveExpiredContent(anyList());

        // When
        scheduler.archiveExpiredContent();

        // Then
        verify(contentService).getExpiredActiveContent();
        verify(contentService).archiveExpiredContent(List.of(expiredId));
        verify(eventPublisher).publishContentArchived(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should not archive when no expired content exists")
    void shouldNotArchiveWhenNoExpiredContentExists() {
        // Given
        when(contentService.getExpiredActiveContent())
                .thenReturn(Collections.emptyList());

        // When
        scheduler.archiveExpiredContent();

        // Then
        verify(contentService).getExpiredActiveContent();
        verify(contentService, never()).archiveExpiredContent(anyList());
        verify(eventPublisher, never()).publishContentArchived(any(ContentEntity.class));
    }

    @Test
    @DisplayName("Should archive multiple expired items")
    void shouldArchiveMultipleExpiredItems() {
        // Given
        UUID secondId = UUID.randomUUID();
        ContentEntity secondExpired = ContentEntity.builder()
                .id(secondId)
                .contentType("ALERT")
                .title("Second Expired Alert")
                .actionType("DISMISS")
                .startDate(LocalDate.now().minusDays(60))
                .endDate(LocalDate.now().minusDays(2))
                .priority(30)
                .status(ContentStatus.ACTIVE)
                .version(1)
                .build();

        when(contentService.getExpiredActiveContent())
                .thenReturn(List.of(expiredContent, secondExpired));

        // When
        scheduler.archiveExpiredContent();

        // Then
        verify(contentService).archiveExpiredContent(List.of(expiredId, secondId));
        verify(eventPublisher, times(2)).publishContentArchived(any(ContentEntity.class));
    }
}
