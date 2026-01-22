package id.payu.compliance.unit;

import id.payu.compliance.application.service.DataAccessAuditService;
import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import id.payu.compliance.domain.port.in.DataAccessAuditUseCase;
import id.payu.compliance.domain.port.out.DataAccessAuditPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataAccessAuditServiceTest {

    @Mock
    private DataAccessAuditPersistencePort persistencePort;

    private DataAccessAuditUseCase dataAccessAuditUseCase;

    @BeforeEach
    void setUp() {
        dataAccessAuditUseCase = new DataAccessAuditService(persistencePort);
    }

    @Test
    void shouldLogDataAccessWithBasicParameters() {
        String userId = "user123";
        String accessedBy = "admin";
        String serviceName = "account-service";
        String resourceType = "User";
        String resourceId = "user123";
        DataOperationType operationType = DataOperationType.READ;
        String purpose = "User profile viewing";

        dataAccessAuditUseCase.logDataAccess(
                userId,
                accessedBy,
                serviceName,
                resourceType,
                resourceId,
                operationType,
                purpose
        );

        verify(persistencePort, times(1)).save(any(DataAccessAudit.class));
    }

    @Test
    void shouldLogDataAccessWithFullParameters() {
        String userId = "user123";
        String accessedBy = "admin";
        String serviceName = "account-service";
        String resourceType = "User";
        String resourceId = "user123";
        DataOperationType operationType = DataOperationType.READ;
        String purpose = "User profile viewing";
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        boolean success = true;
        String errorMessage = null;

        dataAccessAuditUseCase.logDataAccess(
                userId,
                accessedBy,
                serviceName,
                resourceType,
                resourceId,
                operationType,
                purpose,
                ipAddress,
                userAgent,
                success,
                errorMessage
        );

        ArgumentCaptor<DataAccessAudit> captor = ArgumentCaptor.forClass(DataAccessAudit.class);
        verify(persistencePort, times(1)).save(captor.capture());

        DataAccessAudit savedAudit = captor.getValue();
        assertEquals(userId, savedAudit.getUserId());
        assertEquals(accessedBy, savedAudit.getAccessedBy());
        assertEquals(serviceName, savedAudit.getServiceName());
        assertEquals(resourceType, savedAudit.getResourceType());
        assertEquals(resourceId, savedAudit.getResourceId());
        assertEquals(operationType, savedAudit.getOperationType());
        assertEquals(purpose, savedAudit.getPurpose());
        assertEquals(ipAddress, savedAudit.getIpAddress());
        assertEquals(userAgent, savedAudit.getUserAgent());
        assertEquals(success, savedAudit.getSuccess());
        assertEquals(errorMessage, savedAudit.getErrorMessage());
        assertNotNull(savedAudit.getAccessedAt());
    }

    @Test
    void shouldLogFailedDataAccess() {
        String userId = "user123";
        String accessedBy = "admin";
        String serviceName = "account-service";
        String resourceType = "User";
        String resourceId = "user123";
        DataOperationType operationType = DataOperationType.READ;
        String purpose = "User profile viewing";
        String errorMessage = "Access denied - insufficient permissions";

        dataAccessAuditUseCase.logDataAccess(
                userId,
                accessedBy,
                serviceName,
                resourceType,
                resourceId,
                operationType,
                purpose,
                null,
                null,
                false,
                errorMessage
        );

        ArgumentCaptor<DataAccessAudit> captor = ArgumentCaptor.forClass(DataAccessAudit.class);
        verify(persistencePort, times(1)).save(captor.capture());

        DataAccessAudit savedAudit = captor.getValue();
        assertFalse(savedAudit.getSuccess());
        assertEquals(errorMessage, savedAudit.getErrorMessage());
    }

    @Test
    void shouldRetrieveDataAccessAuditById() {
        UUID auditId = UUID.randomUUID();
        DataAccessAudit expectedAudit = DataAccessAudit.builder()
                .id(auditId)
                .userId("user123")
                .accessedBy("admin")
                .serviceName("account-service")
                .resourceType("User")
                .operationType(DataOperationType.READ)
                .accessedAt(LocalDateTime.now())
                .build();

        when(persistencePort.findById(auditId)).thenReturn(List.of(expectedAudit));

        DataAccessAudit result = dataAccessAuditUseCase.getDataAccessAudit(auditId);

        assertNotNull(result);
        assertEquals(auditId, result.getId());
        assertEquals("user123", result.getUserId());
        verify(persistencePort, times(1)).findById(auditId);
    }

    @Test
    void shouldThrowExceptionWhenDataAccessAuditNotFound() {
        UUID auditId = UUID.randomUUID();

        when(persistencePort.findById(auditId)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> {
            dataAccessAuditUseCase.getDataAccessAudit(auditId);
        });

        verify(persistencePort, times(1)).findById(auditId);
    }

    @Test
    void shouldGetUserDataAccessHistory() {
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

        DataAccessAudit audit2 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accessedBy("user123")
                .serviceName("wallet-service")
                .operationType(DataOperationType.READ)
                .accessedAt(LocalDateTime.now().minusHours(1))
                .build();

        Page<DataAccessAudit> expectedPage = new PageImpl<>(List.of(audit1, audit2), pageable, 2);
        when(persistencePort.findByUserId(userId, pageable)).thenReturn(expectedPage);

        Page<DataAccessAudit> result = dataAccessAuditUseCase.getUserDataAccessHistory(userId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(userId, result.getContent().get(0).getUserId());
        verify(persistencePort, times(1)).findByUserId(userId, pageable);
    }

    @Test
    void shouldGetUserDataAccessHistoryByDateRange() {
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

        when(persistencePort.findByUserIdAndDateRange(userId, startDate, endDate))
                .thenReturn(List.of(audit1));

        List<DataAccessAudit> result = dataAccessAuditUseCase.getUserDataAccessHistoryByDateRange(
                userId, startDate, endDate
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        verify(persistencePort, times(1)).findByUserIdAndDateRange(userId, startDate, endDate);
    }

    @Test
    void shouldGetAccessedByUserHistory() {
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

        when(persistencePort.findByAccessedByAndDateRange(accessedBy, startDate, endDate))
                .thenReturn(List.of(audit1));

        List<DataAccessAudit> result = dataAccessAuditUseCase.getAccessedByUserHistory(
                accessedBy, startDate, endDate
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(accessedBy, result.get(0).getAccessedBy());
        verify(persistencePort, times(1)).findByAccessedByAndDateRange(accessedBy, startDate, endDate);
    }

    @Test
    void shouldGetDataAccessByOperationType() {
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
        when(persistencePort.findByOperationType(operationType, pageable)).thenReturn(expectedPage);

        Page<DataAccessAudit> result = dataAccessAuditUseCase.getDataAccessByOperationType(
                operationType, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(operationType, result.getContent().get(0).getOperationType());
        verify(persistencePort, times(1)).findByOperationType(operationType, pageable);
    }

    @Test
    void shouldGetServiceDataAccessHistory() {
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

        when(persistencePort.findByServiceNameAndDateRange(serviceName, startDate, endDate))
                .thenReturn(List.of(audit1));

        List<DataAccessAudit> result = dataAccessAuditUseCase.getServiceDataAccessHistory(
                serviceName, startDate, endDate
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(serviceName, result.get(0).getServiceName());
        verify(persistencePort, times(1)).findByServiceNameAndDateRange(serviceName, startDate, endDate);
    }

    @Test
    void shouldGetUserDataAccessCount() {
        String userId = "user123";
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long expectedCount = 15L;

        when(persistencePort.countByUserIdSinceDate(userId, since)).thenReturn(expectedCount);

        long result = dataAccessAuditUseCase.getUserDataAccessCount(userId, since);

        assertEquals(expectedCount, result);
        verify(persistencePort, times(1)).countByUserIdSinceDate(userId, since);
    }

    @Test
    void shouldGetFailedAccessAttempts() {
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

        when(persistencePort.findFailedAccessAttemptsSince(since)).thenReturn(List.of(audit1));

        List<DataAccessAudit> result = dataAccessAuditUseCase.getFailedAccessAttempts(since);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getSuccess());
        verify(persistencePort, times(1)).findFailedAccessAttemptsSince(since);
    }

    @Test
    void shouldSearchDataAccessAudit() {
        String userId = "user123";
        String serviceName = "account-service";
        DataOperationType operationType = DataOperationType.READ;
        Pageable pageable = PageRequest.of(0, 20);

        DataAccessAudit audit1 = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accessedBy("admin")
                .serviceName(serviceName)
                .operationType(operationType)
                .accessedAt(LocalDateTime.now())
                .build();

        Page<DataAccessAudit> expectedPage = new PageImpl<>(List.of(audit1), pageable, 1);
        when(persistencePort.findByFilters(userId, null, serviceName, operationType, null, null, pageable))
                .thenReturn(expectedPage);

        Page<DataAccessAudit> result = dataAccessAuditUseCase.searchDataAccessAudit(
                userId, null, serviceName, operationType, null, null, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(userId, result.getContent().get(0).getUserId());
        verify(persistencePort, times(1)).findByFilters(userId, null, serviceName, operationType, null, null, pageable);
    }

    @Test
    void shouldDeleteDataAccessAudit() {
        UUID auditId = UUID.randomUUID();

        dataAccessAuditUseCase.deleteDataAccessAudit(auditId);

        verify(persistencePort, times(1)).deleteById(auditId);
    }
}