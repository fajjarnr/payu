package id.payu.cms.adapter.web.rest;

import id.payu.cms.domain.dto.ContentListResponse;
import id.payu.cms.domain.dto.ContentRequest;
import id.payu.cms.domain.dto.ContentResponse;
import id.payu.cms.application.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Content Management
 * Implements adapter layer in hexagonal architecture
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
@Tag(name = "Content Management", description = "CMS APIs for managing banners, promos, and alerts")
@SecurityRequirement(name = "keycloak")
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    @Operation(
        summary = "Create new content",
        description = "Create a new content (banner, promo, alert, or popup)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Content created successfully",
            content = @Content(schema = @Schema(implementation = ContentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Content with same title already exists")
    })
    public ResponseEntity<ContentResponse> createContent(
        @Valid @RequestBody ContentRequest request,
        Principal principal
    ) {
        String createdBy = principal != null ? principal.getName() : "system";
        ContentResponse response = contentService.createContent(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update content",
        description = "Update an existing content by ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Content updated successfully",
            content = @Content(schema = @Schema(implementation = ContentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Content not found"),
        @ApiResponse(responseCode = "409", description = "Content with same title already exists")
    })
    public ResponseEntity<ContentResponse> updateContent(
        @Parameter(description = "Content ID", required = true)
        @PathVariable UUID id,
        @Valid @RequestBody ContentRequest request,
        Principal principal
    ) {
        String updatedBy = principal != null ? principal.getName() : "system";
        ContentResponse response = contentService.updateContent(id, request, updatedBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get content by ID",
        description = "Retrieve a specific content by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Content retrieved successfully",
            content = @Content(schema = @Schema(implementation = ContentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Content not found")
    })
    public ResponseEntity<ContentResponse> getContentById(
        @Parameter(description = "Content ID", required = true)
        @PathVariable UUID id
    ) {
        ContentResponse response = contentService.getContentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all content",
        description = "Retrieve paginated list of all content"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Content list retrieved successfully",
            content = @Content(schema = @Schema(implementation = ContentListResponse.class))
        )
    })
    public ResponseEntity<ContentListResponse> getAllContent(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "Sort field", example = "createdAt")
        @RequestParam(defaultValue = "createdAt") String sortBy,

        @Parameter(description = "Sort direction (asc/desc)", example = "desc")
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        ContentListResponse response = contentService.getAllContent(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "Get content by type",
        description = "Retrieve all content of a specific type (BANNER, PROMO, ALERT, POPUP)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Content list retrieved successfully"
        )
    })
    public ResponseEntity<List<ContentResponse>> getContentByType(
        @Parameter(description = "Content type", required = true, example = "BANNER")
        @PathVariable String type
    ) {
        List<ContentResponse> response = contentService.getContentByType(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "Get content by status",
        description = "Retrieve all content with a specific status (DRAFT, SCHEDULED, ACTIVE, PAUSED, ARCHIVED)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Content list retrieved successfully"
        )
    })
    public ResponseEntity<List<ContentResponse>> getContentByStatus(
        @Parameter(description = "Content status", required = true, example = "ACTIVE")
        @PathVariable String status
    ) {
        List<ContentResponse> response = contentService.getContentByStatus(status);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(
        summary = "Update content status",
        description = "Update the status of a content (e.g., DRAFT -> ACTIVE)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status updated successfully",
            content = @Content(schema = @Schema(implementation = ContentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Content not found"),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<ContentResponse> updateContentStatus(
        @Parameter(description = "Content ID", required = true)
        @PathVariable UUID id,

        @Parameter(description = "New status", required = true, example = "ACTIVE")
        @RequestParam String status,

        Principal principal
    ) {
        String updatedBy = principal != null ? principal.getName() : "system";
        ContentResponse response = contentService.updateContentStatus(id, status, updatedBy);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete content",
        description = "Permanently delete a content by ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Content deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Content not found")
    })
    public ResponseEntity<Void> deleteContent(
        @Parameter(description = "Content ID", required = true)
        @PathVariable UUID id
    ) {
        contentService.deleteContent(id);
        return ResponseEntity.noContent().build();
    }
}
