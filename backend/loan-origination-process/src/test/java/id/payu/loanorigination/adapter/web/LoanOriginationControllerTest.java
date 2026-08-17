package id.payu.loanorigination.adapter.web;

import id.payu.loanorigination.domain.LoanOriginationProcess;
import id.payu.loanorigination.domain.LoanOriginationRequest;
import id.payu.loanorigination.service.LoanOriginationProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanOriginationControllerTest {

    @Mock
    private LoanOriginationProcessService processService;

    @Mock
    private Jwt jwt;

    private LoanOriginationController newController() {
        return new LoanOriginationController(processService);
    }

    private LoanOriginationProcess entity(UUID id) {
        return LoanOriginationProcess.builder()
                .id(id)
                .userId("u-1")
                .principalAmount(new BigDecimal("100000"))
                .loanType("PERSONAL_LOAN")
                .tenureMonths(12)
                .status("PENDING_APPROVAL")
                .build();
    }

    @Test
    void startProcessReturnsOk() {
        var id = UUID.randomUUID();
        when(jwt.getClaimAsString("account_id")).thenReturn("acct-1");
        when(processService.startProcess(any(), eq("acct-1"))).thenReturn(entity(id));
        var controller = newController();

        var response = controller.startProcess(
                new LoanOriginationRequest("u", new BigDecimal("100000"), 12, "home", "PERSONAL_LOAN"),
                jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("processId", id);
        assertThat(response.getBody()).containsEntry("status", "PENDING_APPROVAL");
    }

    @Test
    void startProcessReturnsBadRequestOnValidation() {
        when(jwt.getClaimAsString("account_id")).thenReturn("acct-1");
        when(processService.startProcess(any(), eq("acct-1")))
                .thenThrow(new IllegalArgumentException("Principal amount must be positive"));
        var controller = newController();

        var response = controller.startProcess(
                new LoanOriginationRequest("u", new BigDecimal("0"), 12, "home", "PERSONAL_LOAN"),
                jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Principal amount must be positive");
    }

    @Test
    void startProcessUsesSubjectWhenNoAccountClaim() {
        when(jwt.getClaimAsString("account_id")).thenReturn(null);
        when(jwt.getSubject()).thenReturn("sub-1");
        when(processService.startProcess(any(), eq("sub-1"))).thenReturn(entity(UUID.randomUUID()));
        var controller = newController();

        var response = controller.startProcess(
                new LoanOriginationRequest("u", new BigDecimal("100000"), 12, "home", "PERSONAL_LOAN"),
                jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void startProcessRejectsNullJwt() {
        var controller = newController();

        var response = controller.startProcess(
                new LoanOriginationRequest("u", new BigDecimal("100000"), 12, "home", "PERSONAL_LOAN"),
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Authenticated user is required");
    }

    @Test
    void getProcessReturnsOk() {
        var id = UUID.randomUUID();
        when(processService.getProcess(id)).thenReturn(Optional.of(entity(id)));
        var controller = newController();

        var response = controller.getProcess(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("processId", id);
    }

    @Test
    void getProcessReturnsNotFound() {
        var id = UUID.randomUUID();
        when(processService.getProcess(id)).thenReturn(Optional.empty());
        var controller = newController();

        var response = controller.getProcess(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void approveReturnsOk() {
        var id = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn("officer-1");
        when(processService.approve(id, true, "ok", "officer-1")).thenReturn(entity(id));
        var controller = newController();

        var response = controller.approveTask(id, true, "ok", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void approveReturnsNotFound() {
        var id = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn("officer-1");
        when(processService.approve(id, true, "ok", "officer-1"))
                .thenThrow(new java.util.NoSuchElementException("not found"));
        var controller = newController();

        var response = controller.approveTask(id, true, "ok", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void approveReturnsConflict() {
        var id = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn("officer-1");
        when(processService.approve(id, true, "ok", "officer-1"))
                .thenThrow(new IllegalStateException("Loan process is already completed"));
        var controller = newController();

        var response = controller.approveTask(id, true, "ok", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "Loan process is already completed");
    }

    @Test
    void listProcessesReturnsIds() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        when(processService.listProcessIds()).thenReturn(List.of(id1, id2));
        var controller = newController();

        var response = controller.listProcesses();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("count", 2);
        assertThat((List<String>) response.getBody().get("processes")).containsExactly(id1.toString(), id2.toString());
    }
}
