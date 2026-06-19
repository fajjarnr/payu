package id.payu.cms.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.payu.cms.adapter.web.rest.ContentController;
import id.payu.cms.application.service.ContentService;
import id.payu.cms.config.GlobalExceptionHandler;
import id.payu.cms.domain.dto.ContentListResponse;
import id.payu.cms.domain.dto.ContentRequest;
import id.payu.cms.domain.dto.ContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller layer tests for ContentController using standalone MockMvc setup.
 * Tests all REST endpoints, authorization rules, and error handling in isolation.
 *
 * @author PayU QA Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentController Tests")
class ContentControllerTest {

    @Mock
    private ContentService contentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final UUID CONTENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Standalone setup - no Spring context needed
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ContentController(contentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private ContentRequest buildValidRequest() {
        return ContentRequest.builder()
                .contentType("BANNER")
                .title("Test Banner")
                .description("Test description")
                .imageUrl("https://cdn.example.com/test.png")
                .actionUrl("https://payu.example.com/test")
                .actionType("LINK")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .priority(100)
                .targetingRules(new HashMap<>())
                .metadata(new HashMap<>())
                .build();
    }

    private ContentResponse buildResponse() {
        return ContentResponse.builder()
                .id(CONTENT_ID)
                .contentType("BANNER")
                .title("Test Banner")
                .description("Test description")
                .imageUrl("https://cdn.example.com/test.png")
                .actionUrl("https://payu.example.com/test")
                .actionType("LINK")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .priority(100)
                .status("DRAFT")
                .targetingRules(new HashMap<>())
                .metadata(new HashMap<>())
                .version(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("admin")
                .updatedBy("admin")
                .active(false)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════
    // POST /api/v1/contents - Create Content
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/contents")
    class CreateContentTests {

        @Test
        @DisplayName("Should create content successfully and return 201")
        void shouldCreateContentSuccessfully() throws Exception {
            ContentResponse response = buildResponse();
            given(contentService.createContent(any(ContentRequest.class), eq("system")))
                    .willReturn(response);

            mockMvc.perform(post("/api/v1/contents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Test Banner"))
                    .andExpect(jsonPath("$.contentType").value("BANNER"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturn400WhenTitleIsBlank() throws Exception {
            ContentRequest invalidRequest = ContentRequest.builder()
                    .contentType("BANNER")
                    .title("")
                    .build();

            mockMvc.perform(post("/api/v1/contents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when contentType is blank")
        void shouldReturn400WhenContentTypeIsBlank() throws Exception {
            ContentRequest invalidRequest = ContentRequest.builder()
                    .contentType("")
                    .title("Some Title")
                    .build();

            mockMvc.perform(post("/api/v1/contents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when duplicate title is used")
        void shouldReturn400WhenDuplicateTitleIsUsed() throws Exception {
            given(contentService.createContent(any(ContentRequest.class), anyString()))
                    .willThrow(new IllegalArgumentException("Content with title 'Test Banner' already exists"));

            mockMvc.perform(post("/api/v1/contents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // PUT /api/v1/contents/{id} - Update Content
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/contents/{id}")
    class UpdateContentTests {

        @Test
        @DisplayName("Should update content successfully")
        void shouldUpdateContentSuccessfully() throws Exception {
            ContentResponse response = buildResponse();
            response.setTitle("Updated Banner");
            given(contentService.updateContent(eq(CONTENT_ID), any(ContentRequest.class), eq("system")))
                    .willReturn(response);

            mockMvc.perform(put("/api/v1/contents/{id}", CONTENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Banner"));
        }

        @Test
        @DisplayName("Should return 400 when updating non-existent content")
        void shouldReturn400WhenUpdatingNonExistentContent() throws Exception {
            given(contentService.updateContent(eq(CONTENT_ID), any(ContentRequest.class), eq("system")))
                    .willThrow(new IllegalArgumentException("Content not found with ID: " + CONTENT_ID));

            mockMvc.perform(put("/api/v1/contents/{id}", CONTENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // GET /api/v1/contents/{id} - Get Content By ID
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/contents/{id}")
    class GetContentByIdTests {

        @Test
        @DisplayName("Should get content by ID successfully")
        void shouldGetContentByIdSuccessfully() throws Exception {
            ContentResponse response = buildResponse();
            given(contentService.getContentById(CONTENT_ID)).willReturn(response);

            mockMvc.perform(get("/api/v1/contents/{id}", CONTENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CONTENT_ID.toString()))
                    .andExpect(jsonPath("$.title").value("Test Banner"));
        }

        @Test
        @DisplayName("Should return 400 when content not found")
        void shouldReturn400WhenContentNotFound() throws Exception {
            given(contentService.getContentById(CONTENT_ID))
                    .willThrow(new IllegalArgumentException("Content not found with ID: " + CONTENT_ID));

            mockMvc.perform(get("/api/v1/contents/{id}", CONTENT_ID))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // GET /api/v1/contents - Get All Content (Paginated)
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/contents")
    class GetAllContentTests {

        @Test
        @DisplayName("Should get paginated content list with defaults")
        void shouldGetPaginatedContentListWithDefaults() throws Exception {
            ContentListResponse listResponse = ContentListResponse.builder()
                    .contents(List.of(buildResponse()))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();
            given(contentService.getAllContent(0, 20, "createdAt", "desc"))
                    .willReturn(listResponse);

            mockMvc.perform(get("/api/v1/contents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contents").isArray())
                    .andExpect(jsonPath("$.contents.length()").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should support custom pagination parameters")
        void shouldSupportCustomPaginationParameters() throws Exception {
            ContentListResponse listResponse = ContentListResponse.empty();
            given(contentService.getAllContent(1, 5, "title", "asc"))
                    .willReturn(listResponse);

            mockMvc.perform(get("/api/v1/contents")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sortBy", "title")
                            .param("sortDirection", "asc"))
                    .andExpect(status().isOk());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // GET /api/v1/contents/type/{type}
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/contents/type/{type}")
    class GetContentByTypeTests {

        @Test
        @DisplayName("Should get content by type")
        void shouldGetContentByType() throws Exception {
            given(contentService.getContentByType("BANNER"))
                    .willReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/v1/contents/type/BANNER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].contentType").value("BANNER"));
        }

        @Test
        @DisplayName("Should return empty list for unknown type")
        void shouldReturnEmptyListForUnknownType() throws Exception {
            given(contentService.getContentByType("UNKNOWN"))
                    .willReturn(List.of());

            mockMvc.perform(get("/api/v1/contents/type/UNKNOWN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // GET /api/v1/contents/status/{status}
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/contents/status/{status}")
    class GetContentByStatusTests {

        @Test
        @DisplayName("Should get content by status")
        void shouldGetContentByStatus() throws Exception {
            given(contentService.getContentByStatus("active"))
                    .willReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/v1/contents/status/active"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 for invalid status")
        void shouldReturn400ForInvalidStatus() throws Exception {
            given(contentService.getContentByStatus("INVALID"))
                    .willThrow(new IllegalArgumentException("No enum constant"));

            mockMvc.perform(get("/api/v1/contents/status/INVALID"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/contents/{id}/status
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/v1/contents/{id}/status")
    class UpdateContentStatusTests {

        @Test
        @DisplayName("Should update content status successfully")
        void shouldUpdateContentStatus() throws Exception {
            ContentResponse response = buildResponse();
            response.setStatus("ACTIVE");
            given(contentService.updateContentStatus(CONTENT_ID, "ACTIVE", "system"))
                    .willReturn(response);

            mockMvc.perform(patch("/api/v1/contents/{id}/status", CONTENT_ID)
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 400 for invalid status value")
        void shouldReturn400ForInvalidStatus() throws Exception {
            given(contentService.updateContentStatus(CONTENT_ID, "INVALID", "system"))
                    .willThrow(new IllegalArgumentException("No enum constant"));

            mockMvc.perform(patch("/api/v1/contents/{id}/status", CONTENT_ID)
                            .param("status", "INVALID"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/contents/{id}
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/contents/{id}")
    class DeleteContentTests {

        @Test
        @DisplayName("Should delete content successfully and return 204")
        void shouldDeleteContentSuccessfully() throws Exception {
            mockMvc.perform(delete("/api/v1/contents/{id}", CONTENT_ID))
                    .andExpect(status().isNoContent());

            verify(contentService).deleteContent(CONTENT_ID);
        }

        @Test
        @DisplayName("Should return 400 when deleting non-existent content")
        void shouldReturn400WhenDeletingNonExistentContent() throws Exception {
            doThrow(new IllegalArgumentException("Content not found with ID: " + CONTENT_ID))
                    .when(contentService).deleteContent(CONTENT_ID);

            mockMvc.perform(delete("/api/v1/contents/{id}", CONTENT_ID))
                    .andExpect(status().isBadRequest());
        }
    }
}
