package id.payu.compliance.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.compliance.adapter.web.GdprAuditController;
import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import id.payu.compliance.domain.port.in.DataAccessAuditUseCase;
import id.payu.compliance.dto.DataAccessAuditRequest;
import id.payu.compliance.dto.DataAccessAuditSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GdprAuditControllerTest {

    @Mock
    private DataAccessAuditUseCase dataAccessAuditUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        GdprAuditController controller = new GdprAuditController(dataAccessAuditUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldLogDataAccessSuccessfully() throws Exception {
        DataAccessAuditRequest request = DataAccessAuditRequest.builder()
                .userId("user123")
                .accessedBy("admin")
                .serviceName("account-service")
                .resourceType("User")
                .resourceId("user123")
                .operationType(DataOperationType.READ)
                .purpose("User profile viewing")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .success(true)
                .build();

        doNothing().when(dataAccessAuditUseCase).logDataAccess(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), anyBoolean(), any()
        );

        mockMvc.perform(post("/api/v1/gdpr-audit")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(dataAccessAuditUseCase, times(1)).logDataAccess(
                eq("user123"), eq("admin"), eq("account-service"),
                eq("User"), eq("user123"), eq(DataOperationType.READ),
                eq("User profile viewing"), eq("192.168.1.1"),
                eq("Mozilla/5.0"), eq(true), eq(null)
        );
    }

    @Test
    void shouldGetDataAccessAuditById() throws Exception {
        UUID auditId = UUID.randomUUID();
        DataAccessAudit audit = DataAccessAudit.builder()
                .id(auditId)
                .userId("user123")
                .accessedBy("admin")
                .serviceName("account-service")
                .resourceType("User")
                .operationType(DataOperationType.READ)
                .accessedAt(LocalDateTime.now())
                .build();

        when(dataAccessAuditUseCase.getDataAccessAudit(auditId)).thenReturn(audit);

        mockMvc.perform(get("/api/v1/gdpr-audit/{auditId}", auditId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auditId.toString()))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.accessedBy").value("admin"))
                .andExpect(jsonPath("$.serviceName").value("account-service"))
                .andExpect(jsonPath("$.resourceType").value("User"))
                .andExpect(jsonPath("$.operationType").value("READ"));

        verify(dataAccessAuditUseCase, times(1)).getDataAccessAudit(auditId);
    }

    @Test
    void shouldGetUserDataAccessHistory() throws Exception {
        String userId = "user123";
        Pageable pageable = PageRequest.of(0, 20);

        DataAccessAudit audit1 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accessedBy("admin")
                .serviceName("account-service")
                .operationType(DataOperationType.READ)
                .accessedAt(LocalDateTime.now())
                .build();

        Page<DataAccessAudit> expectedPage = new PageImpl<>(List.of(audit1), pageable, 1);
        when(dataAccessAuditUseCase.getUserDataAccessHistory(eq(userId), any(Pageable.class)))
                .thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/gdpr-audit/users/{userId}", userId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(userId))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(dataAccessAuditUseCase, times(1)).getUserDataAccessHistory(eq(userId), any(Pageable.class));
    }

    @Test
    void shouldGetUserDataAccessByDateRange() throws Exception {
        String userId = "user123";
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        DataAccessAudit audit1 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accessedBy("admin")
                .serviceName("account-service")
                .operationType(DataOperationType.READ)
                .accessedAt(LocalDateTime.now())
                .build();

        when(dataAccessAuditUseCase.getUserDataAccessHistoryByDateRange(userId, startDate, endDate))
                .thenReturn(List.of(audit1));

        mockMvc.perform(get("/api/v1/gdpr-audit/users/{userId}/date-range", userId)
                        .with(user("admin").roles("ADMIN"))
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId));

        verify(dataAccessAuditUseCase, times(1))
                .getUserDataAccessHistoryByDateRange(userId, startDate, endDate);
    }

    @Test
    void shouldGetAccessedByUserHistory() throws Exception {
        String accessedBy = "admin";
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        DataAccessAudit audit1 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId("user123")
                .accessedBy(accessedBy)
                .serviceName("account-service")
                .operationType(DataOperationType.READ)
                .accessedAt(LocalDateTime.now())
                .build();

        when(dataAccessAuditUseCase.getAccessedByUserHistory(accessedBy, startDate, endDate))
                .thenReturn(List.of(audit1));

        mockMvc.perform(get("/api/v1/gdpr-audit/accessed-by/{accessedBy}", accessedBy)
                        .with(user("admin").roles("ADMIN"))
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accessedBy").value(accessedBy));

        verify(dataAccessAuditUseCase, times(1))
                .getAccessedByUserHistory(accessedBy, startDate, endDate);
    }

    @Test
    void shouldGetDataAccessByOperationType() throws Exception {
        DataOperationType operationType = DataOperationType.READ;
        Pageable pageable = PageRequest.of(0, 20);

        DataAccessAudit audit1 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId("user123")
                .accessedBy("admin")
                .serviceName("account-service")
                .operationType(operationType)
                .accessedAt(LocalDateTime.now())
                .build();

        Page<DataAccessAudit> expectedPage = new PageImpl<>(List.of(audit1), pageable, 1);
        when(dataAccessAuditUseCase.getDataAccessByOperationType(eq(operationType), any(Pageable.class)))
                .thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/gdpr-audit/operations/{operationType}", operationType)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].operationType").value("READ"));

        verify(dataAccessAuditUseCase, times(1))
                .getDataAccessByOperationType(eq(operationType), any(Pageable.class));
    }

    @Test
    void shouldGetServiceDataAccessHistory() throws Exception {
        String serviceName = "account-service";
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        DataAccessAudit audit1 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId("user123")
                .accessedBy("admin")
                .serviceName(serviceName)
                .operationType(DataOperationType.READ)
                .accessedAt(LocalDateTime.now())
                .build();

        when(dataAccessAuditUseCase.getServiceDataAccessHistory(serviceName, startDate, endDate))
                .thenReturn(List.of(audit1));

        mockMvc.perform(get("/api/v1/gdpr-audit/services/{serviceName}", serviceName)
                        .with(user("admin").roles("ADMIN"))
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value(serviceName));

        verify(dataAccessAuditUseCase, times(1))
                .getServiceDataAccessHistory(serviceName, startDate, endDate);
    }

    @Test
    void shouldGetUserDataAccessCount() throws Exception {
        String userId = "user123";
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long expectedCount = 15L;

        when(dataAccessAuditUseCase.getUserDataAccessCount(userId, since)).thenReturn(expectedCount);

        mockMvc.perform(get("/api/v1/gdpr-audit/users/{userId}/count", userId)
                        .with(user("admin").roles("ADMIN"))
                        .param("since", since.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(expectedCount)));

        verify(dataAccessAuditUseCase, times(1)).getUserDataAccessCount(userId, since);
    }

    @Test
    void shouldGetFailedAccessAttempts() throws Exception {
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        DataAccessAudit audit1 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId("user123")
                .accessedBy("admin")
                .serviceName("account-service")
                .operationType(DataOperationType.READ)
                .success(false)
                .errorMessage("Access denied")
                .accessedAt(LocalDateTime.now())
                .build();

        when(dataAccessAuditUseCase.getFailedAccessAttempts(since)).thenReturn(List.of(audit1));

        mockMvc.perform(get("/api/v1/gdpr-audit/failed-access")
                        .with(user("admin").roles("ADMIN"))
                        .param("since", since.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].success").value(false))
                .andExpect(jsonPath("$[0].errorMessage").value("Access denied"));

        verify(dataAccessAuditUseCase, times(1)).getFailedAccessAttempts(since);
    }

    @Test
    void shouldSearchDataAccessAudit() throws Exception {
        DataAccessAuditSearchRequest request = DataAccessAuditSearchRequest.builder()
                .userId("user123")
                .serviceName("account-service")
                .operationType(DataOperationType.READ)
                .page(0)
                .size(20)
                .build();

        Pageable pageable = PageRequest.of(0, 20);

        DataAccessAudit audit1 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId("user123")
                .accessedBy("admin")
                .serviceName("account-service")
                .operationType(DataOperationType.READ)
                .accessedAt(LocalDateTime.now())
                .build();

        Page<DataAccessAudit> expectedPage = new PageImpl<>(List.of(audit1), pageable, 1);
        when(dataAccessAuditUseCase.searchDataAccessAudit(
                eq("user123"), eq(null), eq("account-service"),
                eq(DataOperationType.READ), eq(null), eq(null), any(Pageable.class)
        )).thenReturn(expectedPage);

        mockMvc.perform(post("/api/v1/gdpr-audit/search")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user123"));

        verify(dataAccessAuditUseCase, times(1)).searchDataAccessAudit(
                eq("user123"), eq(null), eq("account-service"),
                eq(DataOperationType.READ), eq(null), eq(null), any(Pageable.class)
        );
    }

    @Test
    void shouldDeleteDataAccessAudit() throws Exception {
        UUID auditId = UUID.randomUUID();

        doNothing().when(dataAccessAuditUseCase).deleteDataAccessAudit(auditId);

        mockMvc.perform(delete("/api/v1/gdpr-audit/{auditId}", auditId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(dataAccessAuditUseCase, times(1)).deleteDataAccessAudit(auditId);
    }
}