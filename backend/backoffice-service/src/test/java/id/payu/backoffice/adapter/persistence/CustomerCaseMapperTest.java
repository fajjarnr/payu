package id.payu.backoffice.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import id.payu.backoffice.domain.CaseType;
import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.Priority;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerCaseMapperTest {

    private final CustomerCaseMapper mapper = new CustomerCaseMapper();

    @Test
    void roundTripPreservesPersistenceContract() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        CustomerCase domain = CustomerCase.reconstitute(
                id, "user-1", "ACC-1", "CASE-1", CaseType.BILLING_ISSUE,
                Priority.URGENT, "Subject", "Description", CustomerCaseStatus.RESOLVED,
                "Notes", "agent-1", "agent-2", createdAt, createdAt, 1L);

        CustomerCaseEntity entity = mapper.toEntity(domain);
        CustomerCase restored = mapper.toDomain(entity);

        assertEquals(id, restored.getId());
        assertEquals("CASE-1", restored.getCaseNumber());
        assertEquals(CustomerCaseStatus.RESOLVED, restored.getStatus());
        assertEquals("agent-2", restored.getResolvedBy());
        assertEquals(createdAt, restored.getCreatedAt());
    }
}
