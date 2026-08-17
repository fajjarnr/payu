package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.CaseType;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.Priority;
import id.payu.backoffice.interfaces.dto.CustomerCaseRequest;
import id.payu.backoffice.interfaces.dto.CustomerCaseUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerCaseServiceTest {

    @Autowired
    CustomerCaseService customerCaseService;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-" + System.currentTimeMillis();
    }

    // Create Customer Case Tests

    @Test
    @Transactional
    void testCreateCustomerCase_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-001",
                CaseType.TRANSACTION_DISPUTE,
                Priority.HIGH,
                "Unauthorized transaction",
                "I did not make this transaction",
                "Please investigate"
        );

        CustomerCase result = customerCaseService.create(request);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals("ACC-001", result.getAccountNumber());
        assertEquals(CaseType.TRANSACTION_DISPUTE, result.getCaseType());
        assertEquals(Priority.HIGH, result.getPriority());
        assertEquals("Unauthorized transaction", result.getSubject());
        assertEquals("I did not make this transaction", result.getDescription());
        assertEquals("Please investigate", result.getNotes());
        assertEquals(CustomerCaseStatus.OPEN, result.getStatus());
        assertNotNull(result.getCaseNumber());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    @Transactional
    void testCreateCustomerCase_WithDefaultPriority() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-002",
                CaseType.GENERAL_INQUIRY,
                null,
                "General question",
                "How do I change my password?",
                null
        );

        CustomerCase result = customerCaseService.create(request);

        assertNotNull(result);
        assertEquals(Priority.MEDIUM, result.getPriority());
    }

    @Test
    @Transactional
    void testCreateCustomerCase_GeneratesUniqueCaseNumber() {
        CustomerCaseRequest request1 = new CustomerCaseRequest(
                testUserId,
                "ACC-003",
                CaseType.ACCOUNT_ISSUE,
                Priority.LOW,
                "Subject 1",
                "Description 1",
                null
        );

        CustomerCaseRequest request2 = new CustomerCaseRequest(
                testUserId,
                "ACC-004",
                CaseType.TECHNICAL_ISSUE,
                Priority.LOW,
                "Subject 2",
                "Description 2",
                null
        );

        CustomerCase result1 = customerCaseService.create(request1);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        CustomerCase result2 = customerCaseService.create(request2);

        assertNotEquals(result1.getCaseNumber(), result2.getCaseNumber());
    }

    // Query Customer Case Tests

    @Test
    @Transactional
    void testGetById_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-QUERY",
                CaseType.BILLING_ISSUE,
                Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        CustomerCase customerCase = customerCaseService.create(request);

        Optional<CustomerCase> result = customerCaseService.getById(customerCase.getId());

        assertTrue(result.isPresent());
        assertEquals(customerCase.getId(), result.get().getId());
    }

    @Test
    void testGetById_NotFound() {
        Optional<CustomerCase> result = customerCaseService.getById(UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testGetByCaseNumber_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-CASENUM",
                CaseType.BILLING_ISSUE,
                Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        CustomerCase customerCase = customerCaseService.create(request);

        Optional<CustomerCase> result = customerCaseService.getByCaseNumber(customerCase.getCaseNumber());

        assertTrue(result.isPresent());
        assertEquals(customerCase.getCaseNumber(), result.get().getCaseNumber());
    }

    @Test
    void testGetByCaseNumber_NotFound() {
        Optional<CustomerCase> result = customerCaseService.getByCaseNumber("NONEXISTENT-CASE");

        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testGetByUserId_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-USER",
                CaseType.BILLING_ISSUE,
                Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCase> results = customerCaseService.getByUserId(testUserId);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(cc -> cc.getUserId().equals(testUserId)));
    }

    @Test
    @Transactional
    void testListByStatus_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-STATUS",
                CaseType.BILLING_ISSUE,
                Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCase> results = customerCaseService.listByStatus(CustomerCaseStatus.OPEN, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(cc -> cc.getStatus() == CustomerCaseStatus.OPEN));
    }

    @Test
    @Transactional
    void testListByPriority_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-PRIORITY",
                CaseType.BILLING_ISSUE,
                Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCase> results = customerCaseService.listByPriority(Priority.URGENT, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(cc -> cc.getPriority() == Priority.URGENT));
    }

    @Test
    @Transactional
    void testListAll_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-ALL",
                CaseType.BILLING_ISSUE,
                Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCase> results = customerCaseService.listAll(0, 10);

        assertNotNull(results);
        assertTrue(results.size() >= 1);
    }

    // Assign Customer Case Tests

    @Test
    @Transactional
    void testAssign_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-ASSIGN",
                CaseType.TECHNICAL_ISSUE,
                Priority.MEDIUM,
                "App not working",
                "Cannot login to mobile app",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCase result = customerCaseService.assign(customerCase.getId(), "agent1");

        assertNotNull(result);
        assertEquals("agent1", result.getAssignedTo());
        assertEquals(CustomerCaseStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    @Transactional
    void testAssign_NotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            customerCaseService.assign(UUID.randomUUID(), "agent1");
        });
    }

    // Update Customer Case Tests

    @Test
    @Transactional
    void testUpdate_AsResolved() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-UPDATE-1",
                CaseType.OTHER,
                Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCaseStatus.RESOLVED,
                "Issue resolved successfully"
        );

        CustomerCase result = customerCaseService.update(customerCase.getId(), updateRequest, "agent2");

        assertNotNull(result);
        assertEquals(CustomerCaseStatus.RESOLVED, result.getStatus());
        assertEquals("Issue resolved successfully", result.getNotes());
        assertEquals("agent2", result.getResolvedBy());
        assertNotNull(result.getResolvedAt());
    }

    @Test
    @Transactional
    void testUpdate_AsClosed() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-UPDATE-2",
                CaseType.OTHER,
                Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCaseStatus.CLOSED,
                "Case closed - customer satisfied"
        );

        CustomerCase result = customerCaseService.update(customerCase.getId(), updateRequest, "agent3");

        assertEquals(CustomerCaseStatus.CLOSED, result.getStatus());
        assertEquals("Case closed - customer satisfied", result.getNotes());
        assertEquals("agent3", result.getResolvedBy());
        assertNotNull(result.getResolvedAt());
    }

    @Test
    @Transactional
    void testUpdate_AsInProgress() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-UPDATE-3",
                CaseType.OTHER,
                Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCaseStatus.IN_PROGRESS,
                "Working on this issue"
        );

        CustomerCase result = customerCaseService.update(customerCase.getId(), updateRequest, "agent4");

        assertEquals(CustomerCaseStatus.IN_PROGRESS, result.getStatus());
        assertEquals("Working on this issue", result.getNotes());
        assertNull(result.getResolvedBy());
        assertNull(result.getResolvedAt());
    }

    @Test
    @Transactional
    void testUpdate_NotFound() {
        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCaseStatus.RESOLVED,
                "Test"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            customerCaseService.update(UUID.randomUUID(), updateRequest, "agent1");
        });
    }

    // Delete Customer Case Tests

    @Test
    @Transactional
    void testDelete_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-DELETE",
                CaseType.GENERAL_INQUIRY,
                Priority.LOW,
                "Delete test",
                "Testing deletion",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);
        UUID caseId = customerCase.getId();

        customerCaseService.delete(caseId);

        Optional<CustomerCase> result = customerCaseService.getById(caseId);
        assertFalse(result.isPresent());
    }

    // Pagination Tests

    @Test
    @Transactional
    void testListAll_WithPagination() {
        String uniqueUser = "user-paginate-" + System.currentTimeMillis();

        // Create multiple cases
        for (int i = 0; i < 5; i++) {
            CustomerCaseRequest request = new CustomerCaseRequest(
                    uniqueUser,
                    "ACC-PAG-" + i,
                    CaseType.OTHER,
                    Priority.LOW,
                    "Subject " + i,
                    "Description " + i,
                    null
            );
            customerCaseService.create(request);
        }

        // Get first page
        List<CustomerCase> page1 = customerCaseService.listAll(0, 2);
        assertTrue(page1.size() <= 2);

        // Get second page
        List<CustomerCase> page2 = customerCaseService.listAll(1, 2);
        assertTrue(page2.size() <= 2);
    }

    @Test
    @Transactional
    void testListByStatus_WithPagination() {
        String uniqueUser = "user-status-page-" + System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            CustomerCaseRequest request = new CustomerCaseRequest(
                    uniqueUser,
                    "ACC-STATUS-" + i,
                    CaseType.ACCOUNT_ISSUE,
                    Priority.MEDIUM,
                    "Status test " + i,
                    "Description " + i,
                    null
            );
            customerCaseService.create(request);
        }

        List<CustomerCase> page1 = customerCaseService.listByStatus(CustomerCaseStatus.OPEN, 0, 2);
        assertTrue(page1.size() <= 2);

        List<CustomerCase> page2 = customerCaseService.listByStatus(CustomerCaseStatus.OPEN, 1, 2);
        assertTrue(page2.size() <= 2);
    }
}
