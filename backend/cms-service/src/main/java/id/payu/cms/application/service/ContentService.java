package id.payu.cms.application.service;

import id.payu.cms.domain.dto.ContentRequest;
import id.payu.cms.domain.dto.ContentResponse;
import id.payu.cms.domain.dto.ContentListResponse;
import id.payu.cms.domain.entity.Content;
import id.payu.cms.domain.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for Content management
 * Implements business logic and acts as port in hexagonal architecture
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;

    /**
     * Create new content
     */
    @Transactional
    @CacheEvict(value = "contents", allEntries = true)
    public ContentResponse createContent(ContentRequest request, String createdBy) {
        log.info("Creating new content: {}", request.getTitle());

        // Validate unique title
        if (contentRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new IllegalArgumentException(
                "Content with title '" + request.getTitle() + "' already exists"
            );
        }

        Content content = Content.builder()
            .contentType(request.getContentType())
            .title(request.getTitle())
            .description(request.getDescription())
            .imageUrl(request.getImageUrl())
            .actionUrl(request.getActionUrl())
            .actionType(request.getActionType())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .priority(request.getPriority() != null ? request.getPriority() : 0)
            .status(Content.ContentStatus.DRAFT)
            .targetingRules(request.getTargetingRules())
            .metadata(request.getMetadata())
            .version(1)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();

        Content saved = contentRepository.save(content);
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

        Content content = contentRepository.findById(id)
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

        Content saved = contentRepository.save(content);
        log.info("Content updated: {}", id);

        return toResponse(saved);
    }

    /**
     * Get content by ID
     */
    @Cacheable(value = "contents", key = "#id")
    public ContentResponse getContentById(UUID id) {
        Content content = contentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Content not found with ID: " + id));
        return toResponse(content);
    }

    /**
     * Get all content with pagination
     */
    public ContentListResponse getAllContent(int page, int size, String sortBy, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Content> contentPage = contentRepository.findAll(pageable);

        return ContentListResponse.builder()
            .contents(contentPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()))
            .page(contentPage.getNumber())
            .size(contentPage.getSize())
            .totalElements(contentPage.getTotalElements())
            .totalPages(contentPage.getTotalPages())
            .first(contentPage.isFirst())
            .last(contentPage.isLast())
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
        Content.ContentStatus contentStatus = Content.ContentStatus.valueOf(status.toUpperCase());
        return contentRepository.findByStatus(contentStatus).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get active content by type (for public API)
     */
    @Cacheable(value = "activeContents", key = "#type")
    public List<ContentResponse> getActiveContentByType(String type) {
        List<Content> contents = contentRepository.findActiveByContentType(type, LocalDate.now());
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
        Content content = contentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Content not found with ID: " + id));

        Content.ContentStatus newStatus = Content.ContentStatus.valueOf(status.toUpperCase());
        content.setStatus(newStatus);
        content.setUpdatedBy(updatedBy);

        Content saved = contentRepository.save(content);
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
    public List<Content> getScheduledContentToActivate() {
        return contentRepository.findScheduledToActivate(LocalDate.now());
    }

    /**
     * Get expired active content to archive
     */
    public List<Content> getExpiredActiveContent() {
        return contentRepository.findActiveToArchive(LocalDate.now());
    }

    /**
     * Activate scheduled content
     */
    @Transactional
    @CacheEvict(value = {"contents", "activeContents"}, allEntries = true)
    public void activateScheduledContent(List<UUID> contentIds) {
        contentIds.forEach(id -> {
            contentRepository.findById(id).ifPresent(content -> {
                content.setStatus(Content.ContentStatus.ACTIVE);
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
                content.setStatus(Content.ContentStatus.ARCHIVED);
                contentRepository.save(content);
                log.info("Archived expired content: {}", id);
            });
        });
    }

    /**
     * Convert entity to response DTO
     */
    private ContentResponse toResponse(Content content) {
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
