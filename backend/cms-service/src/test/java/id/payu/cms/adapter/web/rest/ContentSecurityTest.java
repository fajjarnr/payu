package id.payu.cms.adapter.web.rest;

import id.payu.cms.interfaces.dto.ContentResponse;
import id.payu.cms.application.service.ContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QAMVP-014 (cms): unauthenticated → 401; authenticated without
 * admin/cms_editor/cms_admin authority → 403 (RBAC); with authority → 201.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/payu_cms",
        "spring.datasource.username=payu",
        "spring.datasource.password=payu_secret",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@Import(id.payu.cms.config.TestSecurityConfig.class)
@DisplayName("QAMVP-014 — cms security: 401/403 RBAC")
class ContentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    private static final String BODY = "{\"contentType\":\"BANNER\",\"title\":\"Launch promo\"}";

    @Test
    @DisplayName("unauthenticated request is rejected with 401")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/contents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated user without admin/cms authority is rejected with 403")
    void missingAuthorityIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/contents")
                        .with(jwt().jwt(j -> j.claim("scope", "read")))
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authenticated cms_editor creates content with 201")
    void cmsEditorCreatesContent() throws Exception {
        ContentResponse response = mock(ContentResponse.class);
        when(contentService.createContent(any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/contents")
                        .with(jwt().authorities(() -> "cms_editor"))
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isCreated());
    }
}
