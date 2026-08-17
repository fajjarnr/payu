package id.payu.backoffice.adapter.web;

import id.payu.backoffice.application.service.CustomerCaseService;
import id.payu.backoffice.application.service.FraudCaseService;
import id.payu.backoffice.application.service.KycReviewService;
import id.payu.backoffice.application.service.UniversalSearchService;
import id.payu.backoffice.interfaces.dto.ApiResponse;
import id.payu.backoffice.interfaces.dto.UniversalSearchRequest;
import id.payu.backoffice.interfaces.dto.UniversalSearchResponse;
import id.payu.backoffice.interfaces.dto.UniversalSearchResponse.SearchResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BackofficeController universal search endpoints.
 * Converted from @Disabled integration test to mock-based unit test (BUG-TEST-053).
 *
 * Tests verify controller behavior by mocking UniversalSearchService.
 * No Docker or Spring context required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Universal Search Resource Tests (Unit)")
class UniversalSearchResourceTest {

    @Mock
    private KycReviewService kycReviewService;

    @Mock
    private FraudCaseService fraudCaseService;

    @Mock
    private CustomerCaseService customerCaseService;

    @Mock
    private UniversalSearchService universalSearchService;

    @InjectMocks
    private BackofficeController controller;

    private SearchResultItem kycItem;
    private SearchResultItem fraudItem;
    private SearchResultItem customerItem;

    @BeforeEach
    void setUp() {
        kycItem = new SearchResultItem(
                "kyc",
                UUID.randomUUID(),
                "Jane Doe",
                "KTP verification - Matched by userId",
                "searchUser123",
                "SEARCHACC123",
                "PENDING",
                LocalDateTime.now(),
                null
        );

        fraudItem = new SearchResultItem(
                "fraud",
                UUID.randomUUID(),
                "Suspicious Activity",
                "Suspicious transaction pattern - Matched by userId",
                "searchUser123",
                "SEARCHACC123",
                "OPEN",
                LocalDateTime.now(),
                null
        );

        customerItem = new SearchResultItem(
                "customer",
                UUID.randomUUID(),
                "Account locked",
                "Unable to access account - Matched by caseNumber",
                "searchUser456",
                "SEARCHACC456",
                "OPEN",
                LocalDateTime.now(),
                null
        );
    }

    @Test
    @DisplayName("POST /search returns results for matching query")
    void testSearchWithPostRequest() {
        var searchResponse = new UniversalSearchResponse(
                "searchUser123", 0, 20, 2, List.of(kycItem, fraudItem));
        when(universalSearchService.search("searchUser123", null, 0, 20))
                .thenReturn(searchResponse);

        var request = new UniversalSearchRequest("searchUser123");
        ResponseEntity<ApiResponse<UniversalSearchResponse>> response = controller.search(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();

        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.query()).isEqualTo("searchUser123");
        assertThat(data.totalResults()).isEqualTo(2);
        assertThat(data.results()).isNotEmpty();

        verify(universalSearchService).search("searchUser123", null, 0, 20);
    }

    @Test
    @DisplayName("GET /search returns results for matching query")
    void testSearchWithGetRequest() {
        var searchResponse = new UniversalSearchResponse(
                "searchUser123", 0, 20, 2, List.of(kycItem, fraudItem));
        when(universalSearchService.search("searchUser123", null, 0, 20))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("searchUser123", null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.query()).isEqualTo("searchUser123");
        assertThat(data.totalResults()).isEqualTo(2);
        assertThat(data.results()).hasSize(2);

        verify(universalSearchService).search("searchUser123", null, 0, 20);
    }

    @Test
    @DisplayName("GET /search with entity type filter passes type to service")
    void testSearchWithEntityTypeFilter() {
        var searchResponse = new UniversalSearchResponse(
                "searchUser123", 0, 20, 1, List.of(kycItem));
        when(universalSearchService.search("searchUser123", "kyc", 0, 20))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("searchUser123", "kyc", 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.results()).hasSize(1);
        assertThat(data.results().get(0).type()).isEqualTo("kyc");

        verify(universalSearchService).search("searchUser123", "kyc", 0, 20);
    }

    @Test
    @DisplayName("GET /search with pagination passes page/size to service")
    void testSearchWithPagination() {
        var searchResponse = new UniversalSearchResponse(
                "searchUser", 0, 1, 3, List.of(kycItem));
        when(universalSearchService.search("searchUser", null, 0, 1))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("searchUser", null, 0, 1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.page()).isZero();
        assertThat(data.size()).isEqualTo(1);
        assertThat(data.results()).hasSize(1);

        verify(universalSearchService).search("searchUser", null, 0, 1);
    }

    @Test
    @DisplayName("GET /search by account number returns matching kyc and fraud results")
    void testSearchByAccountNumber() {
        var searchResponse = new UniversalSearchResponse(
                "SEARCHACC123", 0, 20, 2, List.of(kycItem, fraudItem));
        when(universalSearchService.search("SEARCHACC123", null, 0, 20))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("SEARCHACC123", null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.totalResults()).isEqualTo(2);
        assertThat(data.results()).extracting(SearchResultItem::type)
                .containsExactlyInAnyOrder("kyc", "fraud");
    }

    @Test
    @DisplayName("GET /search by document number returns kyc results")
    void testSearchByDocumentNumber() {
        var searchResponse = new UniversalSearchResponse(
                "3209999999990001", 0, 20, 1, List.of(kycItem));
        when(universalSearchService.search("3209999999990001", null, 0, 20))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("3209999999990001", null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.totalResults()).isGreaterThanOrEqualTo(1);
        assertThat(data.results()).extracting(SearchResultItem::type).contains("kyc");
    }

    @Test
    @DisplayName("GET /search by case number returns customer case results")
    void testSearchByCaseNumber() {
        var searchResponse = new UniversalSearchResponse(
                "CASE-SEARCH-123", 0, 20, 1, List.of(customerItem));
        when(universalSearchService.search("CASE-SEARCH-123", null, 0, 20))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("CASE-SEARCH-123", null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.totalResults()).isGreaterThanOrEqualTo(1);
        assertThat(data.results()).extracting(SearchResultItem::type).contains("customer");
    }

    @Test
    @DisplayName("GET /search returns empty results for non-matching query")
    void testSearchNoResults() {
        var searchResponse = new UniversalSearchResponse(
                "nonexistent987654321", 0, 20, 0, List.of());
        when(universalSearchService.search("nonexistent987654321", null, 0, 20))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("nonexistent987654321", null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.totalResults()).isZero();
        assertThat(data.results()).isEmpty();
    }

    @Test
    @DisplayName("GET /search without query parameter returns 400 Bad Request")
    void testSearchWithoutQueryParameter() {
        // The controller checks for null/empty query and returns badRequest
        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet(null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();

        verifyNoInteractions(universalSearchService);
    }

    @Test
    @DisplayName("GET /search with empty query parameter returns 400 Bad Request")
    void testSearchWithEmptyQueryParameter() {
        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("", null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();

        verifyNoInteractions(universalSearchService);
    }

    @Test
    @DisplayName("Search result items have required structure fields")
    void testSearchResultStructure() {
        var searchResponse = new UniversalSearchResponse(
                "searchUser123", 0, 20, 1, List.of(kycItem));
        when(universalSearchService.search("searchUser123", null, 0, 20))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response =
                controller.searchGet("searchUser123", null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SearchResultItem item = response.getBody().data().results().get(0);
        assertThat(item.type()).isNotNull();
        assertThat(item.id()).isNotNull();
        assertThat(item.title()).isNotNull();
        assertThat(item.description()).isNotNull();
        assertThat(item.userId()).isNotNull();
        assertThat(item.accountNumber()).isNotNull();
        assertThat(item.status()).isNotNull();
        assertThat(item.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /search with invalid pagination normalizes page/size via record compact constructor")
    void testSearchPostWithInvalidPagination() {
        // UniversalSearchRequest compact constructor normalizes: page -1 -> 0, size 200 -> 20
        var request = new UniversalSearchRequest("searchUser123", null, null, -1, 200);
        // After normalization: page=0, size=20
        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);

        var searchResponse = new UniversalSearchResponse(
                "searchUser123", 0, 20, 2, List.of(kycItem, fraudItem));
        when(universalSearchService.search("searchUser123", null, 0, 20))
                .thenReturn(searchResponse);

        ResponseEntity<ApiResponse<UniversalSearchResponse>> response = controller.search(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UniversalSearchResponse data = response.getBody().data();
        assertThat(data.page()).isZero();
        assertThat(data.size()).isEqualTo(20);
    }
}
