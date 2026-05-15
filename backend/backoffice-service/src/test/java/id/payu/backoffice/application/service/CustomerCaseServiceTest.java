package id.payu.backoffice.application.service;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import id.payu.backoffice.dto.CustomerCaseRequest;
import id.payu.backoffice.dto.CustomerCaseUpdateRequest;
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
                CustomerCaseEntity.CaseType.TRANSACTION_DISPUTE,
                CustomerCaseEntity.Priority.HIGH,
                "Unauthorized transaction",
                "I did not make this transaction",
                "Please investigate"
        );

        CustomerCaseEntity result = customerCaseService.create(request);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals("ACC-001", result.getAccountNumber());
        assertEquals(CustomerCaseEntity.CaseType.TRANSACTION_DISPUTE, result.getCaseType());
        assertEquals(CustomerCaseEntity.Priority.HIGH, result.getPriority());
        assertEquals("Unauthorized transaction", result.getSubject());
        assertEquals("I did not make this transaction", result.getDescription());
        assertEquals("Please investigate", result.getNotes());
        assertEquals(CustomerCaseEntity.CaseStatus.OPEN, result.getStatus());
        assertNotNull(result.getCaseNumber());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    @Transactional
    void testCreateCustomerCase_WithDefaultPriority() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-002",
                CustomerCaseEntity.CaseType.GENERAL_INQUIRY,
                null,
                "General question",
                "How do I change my password?",
                null
        );

        CustomerCaseEntity result = customerCaseService.create(request);

        assertNotNull(result);
        assertEquals(CustomerCaseEntity.Priority.MEDIUM, result.getPriority());
    }

    @Test
    @Transactional
    void testCreateCustomerCase_GeneratesUniqueCaseNumber() {
        CustomerCaseRequest request1 = new CustomerCaseRequest(
                testUserId,
                "ACC-003",
                CustomerCaseEntity.CaseType.ACCOUNT_ISSUE,
                CustomerCaseEntity.Priority.LOW,
                "Subject 1",
                "Description 1",
                null
        );

        CustomerCaseRequest request2 = new CustomerCaseRequest(
                testUserId,
                "ACC-004",
                CustomerCaseEntity.CaseType.TECHNICAL_ISSUE,
                CustomerCaseEntity.Priority.LOW,
                "Subject 2",
                "Description 2",
                null
        );

        CustomerCaseEntity result1 = customerCaseService.create(request1);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        CustomerCaseEntity result2 = customerCaseService.create(request2);

        assertNotEquals(result1.getCaseNumber(), result2.getCaseNumber());
    }

    // Query Customer Case Tests

    @Test
    @Transactional
    void testGetById_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-QUERY",
                CustomerCaseEntity.CaseType.BILLING_ISSUE,
                CustomerCaseEntity.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);

        Optional<CustomerCaseEntity> result = customerCaseService.getById(customerCase.getId());

        assertTrue(result.isPresent());
        assertEquals(customerCase.getId(), result.get().getId());
    }

    @Test
    void testGetById_NotFound() {
        Optional<CustomerCaseEntity> result = customerCaseService.getById(UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testGetByCaseNumber_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-CASENUM",
                CustomerCaseEntity.CaseType.BILLING_ISSUE,
                CustomerCaseEntity.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);

        Optional<CustomerCaseEntity> result = customerCaseService.getByCaseNumber(customerCase.getCaseNumber());

        assertTrue(result.isPresent());
        assertEquals(customerCase.getCaseNumber(), result.get().getCaseNumber());
    }

    @Test
    void testGetByCaseNumber_NotFound() {
        Optional<CustomerCaseEntity> result = customerCaseService.getByCaseNumber("NONEXISTENT-CASE");

        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testGetByUserId_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-USER",
                CustomerCaseEntity.CaseType.BILLING_ISSUE,
                CustomerCaseEntity.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCaseEntity> results = customerCaseService.getByUserId(testUserId);

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
                CustomerCaseEntity.CaseType.BILLING_ISSUE,
                CustomerCaseEntity.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCaseEntity> results = customerCaseService.listByStatus(CustomerCaseEntity.CaseStatus.OPEN, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(cc -> cc.getStatus() == CustomerCaseEntity.CaseStatus.OPEN));
    }

    @Test
    @Transactional
    void testListByPriority_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-PRIORITY",
                CustomerCaseEntity.CaseType.BILLING_ISSUE,
                CustomerCaseEntity.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCaseEntity> results = customerCaseService.listByPriority(CustomerCaseEntity.Priority.URGENT, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(cc -> cc.getPriority() == CustomerCaseEntity.Priority.URGENT));
    }

    @Test
    @Transactional
    void testListAll_Success() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                testUserId,
                "ACC-ALL",
                CustomerCaseEntity.CaseType.BILLING_ISSUE,
                CustomerCaseEntity.Priority.URGENT,
                "Billing inquiry",
                "Wrong amount charged",
                "Urgent attention needed"
        );

        customerCaseService.create(request);

        List<CustomerCaseEntity> results = customerCaseService.listAll(0, 10);

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
                CustomerCaseEntity.CaseType.TECHNICAL_ISSUE,
                CustomerCaseEntity.Priority.MEDIUM,
                "App not working",
                "Cannot login to mobile app",
                null
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);

        CustomerCaseEntity result = customerCaseService.assign(customerCase.getId(), "agent1");

        assertNotNull(result);
        assertEquals("agent1", result.getAssignedTo());
        assertEquals(CustomerCaseEntity.CaseStatus.IN_PROGRESS, result.getStatus());
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
                CustomerCaseEntity.CaseType.OTHER,
                CustomerCaseEntity.Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCaseEntity.CaseStatus.RESOLVED,
                "Issue resolved successfully"
        );

        CustomerCaseEntity result = customerCaseService.update(customerCase.getId(), updateRequest, "agent2");

        assertNotNull(result);
        assertEquals(CustomerCaseEntity.CaseStatus.RESOLVED, result.getStatus());
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
                CustomerCaseEntity.CaseType.OTHER,
                CustomerCaseEntity.Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCaseEntity.CaseStatus.CLOSED,
                "Case closed - customer satisfied"
        );

        CustomerCaseEntity result = customerCaseService.update(customerCase.getId(), updateRequest, "agent3");

        assertEquals(CustomerCaseEntity.CaseStatus.CLOSED, result.getStatus());
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
                CustomerCaseEntity.CaseType.OTHER,
                CustomerCaseEntity.Priority.LOW,
                "Other issue",
                "Some other problem",
                null
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);

        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCaseEntity.CaseStatus.IN_PROGRESS,
                "Working on this issue"
        );

        CustomerCaseEntity result = customerCaseService.update(customerCase.getId(), updateRequest, "agent4");

        assertEquals(CustomerCaseEntity.CaseStatus.IN_PROGRESS, result.getStatus());
        assertEquals("Working on this issue", result.getNotes());
        assertNull(result.getResolvedBy());
        assertNull(result.getResolvedAt());
    }

    @Test
    @Transactional
    void testUpdate_NotFound() {
        CustomerCaseUpdateRequest updateRequest = new CustomerCaseUpdateRequest(
                CustomerCaseEntity.CaseStatus.RESOLVED,
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
                CustomerCaseEntity.CaseType.GENERAL_INQUIRY,
                CustomerCaseEntity.Priority.LOW,
                "Delete test",
                "Testing deletion",
                null
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);
        UUID caseId = customerCase.getId();

        customerCaseService.delete(caseId);

        Optional<CustomerCaseEntity> result = customerCaseService.getById(caseId);
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
                    CustomerCaseEntity.CaseType.OTHER,
                    CustomerCaseEntity.Priority.LOW,
                    "Subject " + i,
                    "Description " + i,
                    null
            );
            customerCaseService.create(request);
        }

        // Get first page
        List<CustomerCaseEntity> page1 = customerCaseService.listAll(0, 2);
        assertTrue(page1.size() <= 2);

        // Get second page
        List<CustomerCaseEntity> page2 = customerCaseService.listAll(1, 2);
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
                    CustomerCaseEntity.CaseType.ACCOUNT_ISSUE,
                    CustomerCaseEntity.Priority.MEDIUM,
                    "Status test " + i,
                    "Description " + i,
                    null
            );
            customerCaseService.create(request);
        }

        List<CustomerCaseEntity> page1 = customerCaseService.listByStatus(CustomerCaseEntity.CaseStatus.OPEN, 0, 2);
        assertTrue(page1.size() <= 2);

        List<CustomerCaseEntity> page2 = customerCaseService.listByStatus(CustomerCaseEntity.CaseStatus.OPEN, 1, 2);
        assertTrue(page2.size() <= 2);
    }
}
