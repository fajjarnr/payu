package id.payu.statement.adapter.web;

import id.payu.statement.application.service.StatementService;
import id.payu.statement.interfaces.dto.StatementResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ARCH-STATEMENT-001 / ADR-0019: partner statement endpoint coverage.
 */
@DisplayName("PartnerStatementController")
class PartnerStatementControllerTest {

    private final StatementService service = mock(StatementService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new PartnerStatementController(service))
            .build();

    private StatementResponse statement(LocalDate period) {
        return StatementResponse.builder()
                .id(java.util.UUID.randomUUID())
                .customerId("cust-1")
                .accountNumber("1234567890")
                .statementPeriod(period)
                .openingBalance(new BigDecimal("0.0000"))
                .closingBalance(new BigDecimal("100000.0000"))
                .totalCredits(new BigDecimal("100000.0000"))
                .totalDebits(BigDecimal.ZERO)
                .build();
    }

    @Test
    void listStatementsFiltersByDateRange() throws Exception {
        LocalDate jan = LocalDate.of(2026, 1, 31);
        LocalDate mar = LocalDate.of(2026, 3, 31);
        StatementResponse s1 = statement(jan);
        StatementResponse s2 = statement(mar);
        when(service.listStatements(eq("cust-1"), any())).thenReturn(new PageImpl<>(List.of(s1, s2)));

        mvc.perform(get("/v1/partner/statements")
                        .param("customerId", "cust-1")
                        .param("from", "2026-02-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void generateStatementDelegates() throws Exception {
        mvc.perform(post("/v1/partner/statements/generate")
                        .contentType("application/json")
                        .content("{\"customerId\":\"cust-1\",\"accountNumber\":\"1234567890\",\"year\":2026,\"month\":3}"))
                .andExpect(status().isOk());

        verify(service).generateStatement(any());
    }
}
