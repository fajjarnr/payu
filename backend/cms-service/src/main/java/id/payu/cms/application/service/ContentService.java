package id.payu.cms.application.service;

import id.payu.cms.interfaces.dto.ContentRequest;
import id.payu.cms.interfaces.dto.ContentResponse;
import id.payu.cms.interfaces.dto.ContentListResponse;
import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.cms.domain.entity.ContentStatus;
import id.payu.cms.domain.port.out.ContentPersistencePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.validation.ConstraintViolationException;

/**
 * Service layer for Content management
 * Implements business logic and acts as port in hexagonal architecture
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentPersistencePort contentRepository;  // BUG-CMS-HEX-001: use port interface, not JPA repo

    /**
     * Create new content
     */
    @Transactional
    @CacheEvict(value = "contents", allEntries = true)
    @CircuitBreaker(name = "cmsService", fallbackMethod = "createContentFallback")
    @Retry(name = "cmsService")
    public ContentResponse createContent(ContentRequest request, String createdBy) {
        log.info("Creating new content: {}", request.getTitle());

        // Validate unique title
        if (contentRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new IllegalArgumentException(
                "Content with title '" + request.getTitle() + "' already exists"
            );
        }

        ContentEntity content = ContentEntity.builder()
            .contentType(request.getContentType())
            .title(request.getTitle())
            .description(request.getDescription())
            .imageUrl(request.getImageUrl())
            .actionUrl(request.getActionUrl())
            .actionType(request.getActionType())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .priority(request.getPriority() != null ? request.getPriority() : 0)
            .status(ContentStatus.DRAFT)
            .targetingRules(request.getTargetingRules())
            .metadata(request.getMetadata())
            .version(1L)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();

        ContentEntity saved;
        try {
            saved = contentRepository.save(content);
        } catch (DataIntegrityViolationException e) {
            // BUG-BE-057: Handle race condition where concurrent creates both pass existsByTitle
            throw new IllegalStateException(
                "Content with title '" + request.getTitle() + "' already exists (concurrent conflict)", e);
        }
        log.info("Content created with ID: {}", saved.getId());

        return toResponse(saved);
    }

    /**
     * Update existing content
     */
    @Transactional
    @CacheEvict(value = "contents", allEntries = true)
    public ContentResponse updateContent(UUID id, ContentRequest request, String updatedBy) {
        log.info("Updating content: {}", id);

        ContentEntity content = contentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Content not found with ID: " + id));

        // Check title uniqueness if changed
        if (!content.getTitle().equalsIgnoreCase(request.getTitle()) &&
            contentRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new IllegalArgumentException(
                "Content with title '" + request.getTitle() + "' already exists"
            );
        }

        // Update fields
        content.setContentType(request.getContentType());
        content.setTitle(request.getTitle());
        content.setDescription(request.getDescription());
        content.setImageUrl(request.getImageUrl());
        content.setActionUrl(request.getActionUrl());
        content.setActionType(request.getActionType());
        content.setStartDate(request.getStartDate());
        content.setEndDate(request.getEndDate());
        content.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        content.setTargetingRules(request.getTargetingRules());
        content.setMetadata(request.getMetadata());
        content.setVersion(content.getVersion() + 1);
        content.setUpdatedBy(updatedBy);

        ContentEntity saved = contentRepository.save(content);
        log.info("Content updated: {}", id);

        return toResponse(saved);
    }

    /**
     * Get content by ID
     */
    @Cacheable(value = "contents", key = "#id")
    @CircuitBreaker(name = "cmsService", fallbackMethod = "getContentByIdFallback")
    @Retry(name = "cmsService")
    public ContentResponse getContentById(UUID id) {
        ContentEntity content = contentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Content not found with ID: " + id));
        return toResponse(content);
    }

    /**
     * Get all content with pagination
     */
    public ContentListResponse getAllContent(int page, int size, String sortBy, String sortDirection) {
        // BUG-CMS-HEX-001: use port's primitive signature, not Spring's Pageable
        List<ContentEntity> contents = contentRepository.findAll(page, size, sortBy, sortDirection);

        return ContentListResponse.builder()
            .contents(contents.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()))
            .page(page)
            .size(size)
            // Total counts not exposed via port — caller can use count() separately
            .totalElements(contents.size())
            .totalPages(1)
            .first(page == 0)
            .last(contents.size() < size)
            .build();
    }

    /**
     * Get content by type
     */
    @Cacheable(value = "contents", key = "'type:' + #type")
    public List<ContentResponse> getContentByType(String type) {
        return contentRepository.findByContentType(type).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get content by status
     */
    @Cacheable(value = "contents", key = "'status:' + #status")
    public List<ContentResponse> getContentByStatus(String status) {
        ContentStatus contentStatus = ContentStatus.valueOf(status.toUpperCase());
        return contentRepository.findByStatus(contentStatus).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get active content by type (for public API)
     */
    @Cacheable(value = "activeContents", key = "#type")
    public List<ContentResponse> getActiveContentByType(String type) {
        List<ContentEntity> contents = contentRepository.findActiveByContentType(type, LocalDate.now());
        return contents.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Update content status
     */
    @Transactional
    @CacheEvict(value = {"contents", "activeContents"}, allEntries = true)
    public ContentResponse updateContentStatus(UUID id, String status, String updatedBy) {
        ContentEntity content = contentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Content not found with ID: " + id));

        ContentStatus newStatus = ContentStatus.valueOf(status.toUpperCase());
        content.setStatus(newStatus);
        content.setUpdatedBy(updatedBy);

        ContentEntity saved = contentRepository.save(content);
        log.info("Content status updated: {} -> {}", id, status);

        return toResponse(saved);
    }

    /**
     * Delete content
     */
    @Transactional
    @CacheEvict(value = {"contents", "activeContents"}, allEntries = true)
    public void deleteContent(UUID id) {
        if (!contentRepository.existsById(id)) {
            throw new IllegalArgumentException("Content not found with ID: " + id);
        }
        contentRepository.deleteById(id);
        log.info("Content deleted: {}", id);
    }

    /**
     * Get scheduled content to activate
     */
    public List<ContentEntity> getScheduledContentToActivate() {
        return contentRepository.findScheduledToActivate(LocalDate.now());
    }

    /**
     * Get expired active content to archive
     */
    public List<ContentEntity> getExpiredActiveContent() {
        return contentRepository.findActiveToArchive(LocalDate.now());
    }

    // ─── Fallback methods ──────────────────────────────────────────────────────

    private ContentResponse createContentFallback(ContentRequest request, String createdBy, Throwable ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Circuit breaker triggered for createContent [title={}]: {}", request.getTitle(), ex.getMessage());
        throw new IllegalStateException("CMS service temporarily unavailable. Please retry later.", ex);
    }

    private ContentResponse getContentByIdFallback(UUID id, Throwable ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Circuit breaker triggered for getContentById [id={}]: {}", id, ex.getMessage());
        throw new IllegalStateException("CMS service temporarily unavailable. Please retry later.", ex);
    }
    /**
     * Activate scheduled content
     */
    @Transactional
    @CacheEvict(value = {"contents", "activeContents"}, allEntries = true)
    public void activateScheduledContent(List<UUID> contentIds) {
        contentIds.forEach(id -> {
            contentRepository.findById(id).ifPresent(content -> {
                content.setStatus(ContentStatus.ACTIVE);
                contentRepository.save(content);
                log.info("Activated scheduled content: {}", id);
            });
        });
    }

    /**
     * Archive expired content
     */
    @Transactional
    @CacheEvict(value = {"contents", "activeContents"}, allEntries = true)
    public void archiveExpiredContent(List<UUID> contentIds) {
        contentIds.forEach(id -> {
            contentRepository.findById(id).ifPresent(content -> {
                content.setStatus(ContentStatus.ARCHIVED);
                contentRepository.save(content);
                log.info("Archived expired content: {}", id);
            });
        });
    }

    /**
     * Convert entity to response DTO
     */
    private ContentResponse toResponse(ContentEntity content) {
        return ContentResponse.builder()
            .id(content.getId())
            .contentType(content.getContentType())
            .title(content.getTitle())
            .description(content.getDescription())
            .imageUrl(content.getImageUrl())
            .actionUrl(content.getActionUrl())
            .actionType(content.getActionType())
            .startDate(content.getStartDate())
            .endDate(content.getEndDate())
            .priority(content.getPriority())
            .status(content.getStatus().name())
            .targetingRules(content.getTargetingRules())
            .metadata(content.getMetadata())
            .version(content.getVersion())
            .createdAt(content.getCreatedAt())
            .updatedAt(content.getUpdatedAt())
            .createdBy(content.getCreatedBy())
            .updatedBy(content.getUpdatedBy())
            .active(content.isActive())
            .build();
    }
}
