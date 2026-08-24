package id.payu.statement.adapter.web;

import id.payu.statement.application.service.StatementService;
import id.payu.statement.interfaces.dto.StatementResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    void exportCsvViaExportEndpointReturnsValidCsv() throws Exception {
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(3);
            out.write("id,customerId,accountNumber,statementPeriod,openingBalance,closingBalance,totalCredits,totalDebits,transactionCount,status,generatedAt\n".getBytes(StandardCharsets.UTF_8));
            out.write("uuid,cust-1,1234567890,2026-03-01,0.00,100000.00,100000.00,0.00,1,COMPLETED,2026-03-31T00:00:00\n".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(service).exportStatementsCsv(eq("cust-1"), any(), any(), any());

        mvc.perform(get("/v1/partner/statements/export")
                        .param("customerId", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("customerId")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cust-1")));
    }

    @Test
    void exportCsvViaAcceptHeaderReturnsCsv() throws Exception {
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(3);
            out.write("id,customerId,accountNumber,statementPeriod,openingBalance,closingBalance,totalCredits,totalDebits,transactionCount,status,generatedAt\n".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(service).exportStatementsCsv(eq("cust-1"), any(), any(), any());

        mvc.perform(get("/v1/partner/statements")
                        .param("customerId", "cust-1")
                        .header("Accept", "text/csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id,customerId")));
    }

    @Test
    void csvEscapingAndHalfEvenFormatting() {
        assertThat(StatementService.escapeCsv("a,b")).isEqualTo("\"a,b\"");
        assertThat(StatementService.escapeCsv("a\"b")).isEqualTo("\"a\"\"b\"");
        assertThat(StatementService.escapeCsv("a\nb")).isEqualTo("\"a\nb\"");
        assertThat(StatementService.escapeCsv("simple")).isEqualTo("simple");
        assertThat(StatementService.formatMoneyCsv(new BigDecimal("2.345"))).isEqualTo("2.34");
        assertThat(StatementService.formatMoneyCsv(new BigDecimal("2.355"))).isEqualTo("2.36");
        assertThat(StatementService.formatMoneyCsv(null)).isEqualTo("0.00");
    }
}
