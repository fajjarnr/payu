package id.payu.cms.adapter.web.rest;

import id.payu.cms.interfaces.dto.ContentResponse;
import id.payu.cms.application.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public REST Controller for Content retrieval
 * No authentication required - used by mobile/web apps
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public/contents")
@RequiredArgsConstructor
@Tag(name = "Public Content", description = "Public APIs for retrieving active content")
public class PublicContentController {

    private final ContentService contentService;

    @GetMapping
    @Operation(
        summary = "Get public content service info",
        description = "Retrieve public content service status and available endpoints"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Public content service info retrieved successfully"
        )
    })
    public ResponseEntity<Map<String, Object>> getPublicContentInfo() {
        return ResponseEntity.ok(Map.of(
            "status", "operational",
            "service", "cms-service",
            "version", "1.0.0",
            "endpoints", List.of(
                "GET /api/v1/public/contents",
                "GET /api/v1/public/contents/type/{type}"
            )
        ));
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "Get active content by type",
        description = "Retrieve all active content of a specific type (BANNER, PROMO, ALERT, POPUP). " +
                     "Only returns content that is currently within its date range."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active content list retrieved successfully"
        )
    })
    public ResponseEntity<List<ContentResponse>> getActiveContentByType(
        @Parameter(description = "Content type", required = true, example = "BANNER")
        @PathVariable String type,

        @Parameter(description = "User segment for targeting", example = "PREMIUM")
        @RequestParam(required = false) String segment,

        @Parameter(description = "User location for targeting", example = "JAKARTA")
        @RequestParam(required = false) String location,

        @Parameter(description = "Device type for targeting", example = "MOBILE")
        @RequestParam(required = false) String device
    ) {
        List<ContentResponse> contents = contentService.getActiveContentByType(type);

        // Apply additional filtering for targeting if parameters provided
        if (segment != null || location != null || device != null) {
            contents = contents.stream()
                .filter(content -> {
                    // Targeting filtering would be done here based on the targetingRules
                    // For now, return all active content
                    return true;
                })
                .toList();
        }

        return ResponseEntity.ok(contents);
    }
}
