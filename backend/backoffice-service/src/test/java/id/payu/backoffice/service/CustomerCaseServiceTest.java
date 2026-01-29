package id.payu.backoffice.service;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.dto.CustomerCaseRequest;
import id.payu.backoffice.dto.CustomerCaseUpdateRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CustomerCaseServiceTest {

    @Inject
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
                CustomerCase.CaseType.TRANSACTION_DISPUTE,
                CustomerCase.Priority.HIGH,
                "Unauthorized transaction",
                "I did not make this transaction",
                "Please investigate"
        );

        CustomerCase result = customerCaseService.create(request);

        assertNotNull(result);
        assertNotNull(result.id);
        assertEquals(testUserId, result.userId);
        assertEquals("ACC-001", result.accountNumber);
        assertEquals(CustomerCase.CaseType.TRANSACTION_DISPUTE, result.caseType);
        assertEquals(CustomerCase.Priority.HIGH, result.priority);
        assertEquals("Unauthorized transaction", result.subject);
        assertEquals("I did not make this transaction", result.description);
        assertEquals("Please investigate", result.notes);
        assertEquals(CustomerCase.CaseStatus.OPEN, result.status);
        assertNotNull(result.caseNumber);
        assertNotNull(result.createdAt);
    }

    @Test
    @Transactional
    void testCreateCustomerCase_WithDefaultPriority() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-002",
                CustomerCase.CaseType.GENERAL_INQUIRY,
                null,
                "General question",
                "How do I change my password?",
                null
        );

        CustomerCase result = customerCaseService.create(request);

        assertNotNull(result);
        assertEquals(CustomerCase.Priority.MEDIUM, result.priority);
    }

    @Test
    @Transactional
    void testCreateCustomerCase_GeneratesUniqueCaseNumber() {
        CustomerCaseRequest request1 = new CustomerCaseRequest(
                testUserId,
                "ACC-003",
                CustomerCase.CaseType.ACCOUNT_ISSUE,
                CustomerCase.Priority.LOW,
                "Subject 1",
                "Description 1",
                null
        );

        // Small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        CustomerCaseRequest request2 = new CustomerCaseRequest(
                testUserId,
                "ACC-004",
                CustomerCase.CaseType.TECHNICAL_ISSUE,
                CustomerCase.Priority.LOW,
                "Subject 2",
                "Description 2",
                null
        );

        CustomerCase result1 = customerCaseService.create(request1);
        CustomerCase result2 = customerCaseService.create(request2);

        assertNotEquals(result1.caseNumber, result2.caseNumber);
    }

    // Query Customer Case Tests

    @Test
    @Transactional
    void testGetById_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-QUERY",
                CustomerCase.CaseType.BILLING_ISSUE,
                CustomerCase.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        CustomerCase customerCase = customerCaseService.create(request);

        Optional<CustomerCase> result = customerCaseService.getById(customerCase.id);

        assertTrue(result.isPresent());
        assertEquals(customerCase.id, result.get().id);
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
                CustomerCase.CaseType.BILLING_ISSUE,
                CustomerCase.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        CustomerCase customerCase = customerCaseService.create(request);

        Optional<CustomerCase> result = customerCaseService.getByCaseNumber(customerCase.caseNumber);

        assertTrue(result.isPresent());
        assertEquals(customerCase.caseNumber, result.get().caseNumber);
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
                CustomerCase.CaseType.BILLING_ISSUE,
                CustomerCase.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCase> results = customerCaseService.getByUserId(testUserId);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(cc -> cc.userId.equals(testUserId)));
    }

    @Test
    @Transactional
    void testListByStatus_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-STATUS",
                CustomerCase.CaseType.BILLING_ISSUE,
                CustomerCase.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCase> results = customerCaseService.listByStatus(CustomerCase.CaseStatus.OPEN, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(cc -> cc.status == CustomerCase.CaseStatus.OPEN));
    }

    @Test
    @Transactional
    void testListByPriority_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-PRIORITY",
                CustomerCase.CaseType.BILLING_ISSUE,
                CustomerCase.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCase> results = customerCaseService.listByPriority(CustomerCase.Priority.URGENT, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(cc -> cc.priority == CustomerCase.Priority.URGENT));
    }

    @Test
    @Transactional
    void testListAll_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-ALL",
                CustomerCase.CaseType.BILLING_ISSUE,
                CustomerCase.Priority.URGENT,
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
                CustomerCase.CaseType.TECHNICAL_ISSUE,
                CustomerCase.Priority.MEDIUM,
                "App not working",
                "Cannot login to mobile app",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCase result = customerCaseService.assign(customerCase.id, "agent1");

        assertNotNull(result);
        assertEquals("agent1", result.assignedTo);
        assertEquals(CustomerCase.CaseStatus.IN_PROGRESS, result.status);
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
                CustomerCase.CaseType.OTHER,
                CustomerCase.Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.RESOLVED,
                "Issue resolved successfully"
        );

        CustomerCase result = customerCaseService.update(customerCase.id, updateRequest, "agent2");

        assertNotNull(result);
        assertEquals(CustomerCase.CaseStatus.RESOLVED, result.status);
        assertEquals("Issue resolved successfully", result.notes);
        assertEquals("agent2", result.resolvedBy);
        assertNotNull(result.resolvedAt);
    }

    @Test
    @Transactional
    void testUpdate_AsClosed() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-UPDATE-2",
                CustomerCase.CaseType.OTHER,
                CustomerCase.Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.CLOSED,
                "Case closed - customer satisfied"
        );

        CustomerCase result = customerCaseService.update(customerCase.id, updateRequest, "agent3");

        assertEquals(CustomerCase.CaseStatus.CLOSED, result.status);
        assertEquals("Case closed - customer satisfied", result.notes);
        assertEquals("agent3", result.resolvedBy);
        assertNotNull(result.resolvedAt);
    }

    @Test
    @Transactional
    void testUpdate_AsInProgress() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-UPDATE-3",
                CustomerCase.CaseType.OTHER,
                CustomerCase.Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.IN_PROGRESS,
                "Working on this issue"
        );

        CustomerCase result = customerCaseService.update(customerCase.id, updateRequest, "agent4");

        assertEquals(CustomerCase.CaseStatus.IN_PROGRESS, result.status);
        assertEquals("Working on this issue", result.notes);
        assertNull(result.resolvedBy);
        assertNull(result.resolvedAt);
    }

    @Test
    @Transactional
    void testUpdate_NotFound() {
        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.RESOLVED,
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
                CustomerCase.CaseType.GENERAL_INQUIRY,
                CustomerCase.Priority.LOW,
                "Delete test",
                "Testing deletion",
                null
        );

        CustomerCase customerCase = customerCaseService.create(request);
        UUID caseId = customerCase.id;

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
                    CustomerCase.CaseType.OTHER,
                    CustomerCase.Priority.LOW,
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
                    CustomerCase.CaseType.ACCOUNT_ISSUE,
                    CustomerCase.Priority.MEDIUM,
                    "Status test " + i,
                    "Description " + i,
                    null
            );
            customerCaseService.create(request);
        }

        List<CustomerCase> page1 = customerCaseService.listByStatus(CustomerCase.CaseStatus.OPEN, 0, 2);
        assertTrue(page1.size() <= 2);

        List<CustomerCase> page2 = customerCaseService.listByStatus(CustomerCase.CaseStatus.OPEN, 1, 2);
        assertTrue(page2.size() <= 2);
    }
}
