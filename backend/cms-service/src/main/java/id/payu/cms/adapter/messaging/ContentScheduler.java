package id.payu.cms.adapter.messaging;

import id.payu.cms.application.service.ContentService;
import id.payu.cms.domain.entity.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Scheduler for content activation and archival
 * Runs periodically to update content status based on dates
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentScheduler {

    private final ContentService contentService;
    private final ContentEventPublisher eventPublisher;

    /**
     * Activate scheduled content
     * Runs every hour at the top of the hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void activateScheduledContent() {
        log.info("Checking for scheduled content to activate");

        List<Content> scheduledContent = contentService.getScheduledContentToActivate();

        if (scheduledContent.isEmpty()) {
            log.debug("No scheduled content to activate");
            return;
        }

        List<UUID> contentIds = scheduledContent.stream()
            .map(Content::getId)
            .toList();

        log.info("Activating {} scheduled content items", contentIds.size());
        contentService.activateScheduledContent(contentIds);

        // Publish events for newly activated content
        scheduledContent.forEach(content -> {
            content.setStatus(Content.ContentStatus.ACTIVE);
            eventPublisher.publishContentPublished(content);
        });
    }

    /**
     * Archive expired content
     * Runs every hour at 30 minutes past the hour
     */
    @Scheduled(cron = "0 30 * * * *")
    public void archiveExpiredContent() {
        log.info("Checking for expired content to archive");

        List<Content> expiredContent = contentService.getExpiredActiveContent();

        if (expiredContent.isEmpty()) {
            log.debug("No expired content to archive");
            return;
        }

        List<UUID> contentIds = expiredContent.stream()
            .map(Content::getId)
            .toList();

        log.info("Archiving {} expired content items", contentIds.size());
        contentService.archiveExpiredContent(contentIds);

        // Publish events for archived content
        expiredContent.forEach(eventPublisher::publishContentArchived);
    }
}
