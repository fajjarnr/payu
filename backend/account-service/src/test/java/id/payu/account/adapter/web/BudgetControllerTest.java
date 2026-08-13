package id.payu.account.adapter.web;

import id.payu.account.application.service.BudgetService;
import id.payu.account.domain.model.Budget;
import id.payu.account.domain.model.BudgetPeriod;
import id.payu.account.domain.model.BudgetStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ACCOUNT-006: BudgetController coverage.
 */
@DisplayName("BudgetController")
class BudgetControllerTest {

    private final BudgetService service = mock(BudgetService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BudgetController(service)).build();

    private Budget budget(UUID owner) {
        return Budget.builder()
                .id(UUID.randomUUID())
                .userId(owner)
                .category("FOOD")
                .limitAmount(new BigDecimal("1000.0000"))
                .period(BudgetPeriod.MONTHLY)
                .currentSpent(BigDecimal.ZERO)
                .resetDate(LocalDate.now().plusMonths(1))
                .active(true)
                .build();
    }

    @Test
    void createBudget() throws Exception {
        UUID accountId = UUID.randomUUID();
        Budget b = budget(accountId);
        when(service.createBudget(eq(accountId), eq("FOOD"), any(), eq(BudgetPeriod.MONTHLY))).thenReturn(b);

        mvc.perform(post("/api/v1/accounts/{accountId}/budgets", accountId)
                        .contentType("application/json")
                        .content("{\"category\":\"FOOD\",\"limitAmount\":1000,\"period\":\"MONTHLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("FOOD"));
    }

    @Test
    void createBudgetRejectsInvalidBody() throws Exception {
        UUID accountId = UUID.randomUUID();
        mvc.perform(post("/api/v1/accounts/{accountId}/budgets", accountId)
                        .contentType("application/json")
                        .content("{\"category\":\"FOOD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBudgetsLists() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(service.getUserBudgets(accountId)).thenReturn(List.of(budget(accountId)));

        mvc.perform(get("/api/v1/accounts/{accountId}/budgets", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("FOOD"));
    }

    @Test
    void getBudgetFoundAndNotFound() throws Exception {
        UUID accountId = UUID.randomUUID();
        Budget b = budget(accountId);
        when(service.getBudget(accountId, b.getId())).thenReturn(Optional.of(b));
        when(service.getBudget(accountId, UUID.randomUUID())).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/accounts/{accountId}/budgets/{budgetId}", accountId, b.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("FOOD"));

        mvc.perform(get("/api/v1/accounts/{accountId}/budgets/{budgetId}", accountId, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBudget() throws Exception {
        UUID accountId = UUID.randomUUID();
        Budget b = budget(accountId);
        when(service.updateBudget(eq(accountId), eq(b.getId()), eq(new BigDecimal("2000.0000")),
                eq(BudgetPeriod.WEEKLY), eq(true))).thenReturn(b);

        mvc.perform(put("/api/v1/accounts/{accountId}/budgets/{budgetId}", accountId, b.getId())
                        .contentType("application/json")
                        .content("{\"limitAmount\":2000,\"period\":\"WEEKLY\",\"active\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteBudget() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        mvc.perform(delete("/api/v1/accounts/{accountId}/budgets/{budgetId}", accountId, budgetId))
                .andExpect(status().isOk());
    }

    @Test
    void getBudgetStatus() throws Exception {
        UUID accountId = UUID.randomUUID();
        Budget b = budget(accountId);
        BudgetService.BudgetStatusInfo info = new BudgetService.BudgetStatusInfo(
                b.getId(), "FOOD", BudgetStatus.ACTIVE, b.getLimitAmount(), b.getCurrentSpent(),
                BigDecimal.ZERO, b.getLimitAmount(), true);
        when(service.getAllBudgetStatus(accountId)).thenReturn(List.of(info));

        mvc.perform(get("/api/v1/accounts/{accountId}/budgets/status", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("FOOD"));
    }

    @Test
    void checkBudget() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(service.checkBudget(eq(accountId), eq("FOOD"), any()))
                .thenReturn(new BudgetService.BudgetCheckResult(
                        id.payu.account.application.service.BudgetCheckStatus.ALLOWED, null, null));

        mvc.perform(post("/api/v1/accounts/{accountId}/budgets/check", accountId)
                        .contentType("application/json")
                        .content("{\"category\":\"FOOD\",\"amount\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ALLOWED"));
    }
}
