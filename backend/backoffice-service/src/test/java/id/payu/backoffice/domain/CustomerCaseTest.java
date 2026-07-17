package id.payu.backoffice.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerCaseTest {

    @Test
    void newCaseUsesSafeDefaultsAndUniqueNumber() {
        CustomerCase first = CustomerCase.create(
                "user-1", "ACC-1", CaseType.GENERAL_INQUIRY, null,
                "Question", "Details", null);
        CustomerCase second = CustomerCase.create(
                "user-1", "ACC-1", CaseType.GENERAL_INQUIRY, null,
                "Question", "Details", null);

        assertEquals(CustomerCaseStatus.OPEN, first.getStatus());
        assertEquals(Priority.MEDIUM, first.getPriority());
        assertTrue(first.getCaseNumber().startsWith("CASE-"));
        assertNotEquals(first.getCaseNumber(), second.getCaseNumber());
        assertNotNull(first.getCreatedAt());
    }

    @Test
    void assigningOpenCaseStartsWork() {
        CustomerCase customerCase = existing(CustomerCaseStatus.OPEN);

        customerCase.assignTo("agent-1");

        assertEquals("agent-1", customerCase.getAssignedTo());
        assertEquals(CustomerCaseStatus.IN_PROGRESS, customerCase.getStatus());
    }

    @Test
    void resolvingCaseRecordsActorAndTime() {
        CustomerCase customerCase = existing(CustomerCaseStatus.IN_PROGRESS);

        customerCase.update(CustomerCaseStatus.RESOLVED, "Resolved", "agent-2");

        assertEquals(CustomerCaseStatus.RESOLVED, customerCase.getStatus());
        assertEquals("agent-2", customerCase.getResolvedBy());
        assertNotNull(customerCase.getResolvedAt());
    }

    private CustomerCase existing(CustomerCaseStatus status) {
        return CustomerCase.reconstitute(
                UUID.randomUUID(), "user-1", "ACC-1", "CASE-1",
                CaseType.ACCOUNT_ISSUE, Priority.HIGH, "Subject", "Description",
                status, null, null, null, null, LocalDateTime.now(), null);
    }
}
