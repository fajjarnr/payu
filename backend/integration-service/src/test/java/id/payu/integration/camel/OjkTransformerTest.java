package id.payu.integration.camel;

import id.payu.integration.adapter.camel.transformer.OjkTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OjkTransformer.
 */
public class OjkTransformerTest {

    private OjkTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new OjkTransformer();
    }

    @Test
    void testToCsvFormat() {
        Map<String, Object> reportData = Map.of(
                "reportDate", "2024-01-01",
                "reportType", "DAILY_TRANSACTION",
                "institutionCode", "PAYU",
                "totalTransactions", 100,
                "totalAmount", "1000000.00",
                "currency", "IDR"
        );

        String result = transformer.toCsvFormat(reportData);

        assertNotNull(result);
        assertTrue(result.contains("ReportDate,ReportType,InstitutionCode,TotalTransactions,TotalAmount,Currency"));
        assertTrue(result.contains("2024-01-01,DAILY_TRANSACTION,PAYU,100,1000000.00,IDR"));
    }

    @Test
    void testToXmlFormat() {
        Map<String, Object> reportData = Map.of(
                "reportDate", "2024-01-01",
                "reportType", "MONTHLY_SUMMARY",
                "institutionCode", "PAYU",
                "period", "MONTHLY",
                "totalTransactions", 5000,
                "totalAmount", "50000000.00",
                "currency", "IDR",
                "details", List.of()
        );

        String result = transformer.toXmlFormat(reportData);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(result.contains("<OJKReport"));
        assertTrue(result.contains("<InstitutionCode>PAYU</InstitutionCode>"));
        assertTrue(result.contains("<ReportType>MONTHLY_SUMMARY</ReportType>"));
        assertTrue(result.contains("<TotalTransactions>5000</TotalTransactions>"));
        assertTrue(result.contains("</OJKReport>"));
    }

    @Test
    void testFromCsvFormat() {
        String csv = "ReportDate,ReportType,InstitutionCode,TotalTransactions,TotalAmount,Currency\n" +
                "2024-01-01,DAILY,PAYU,100,1000000.00,IDR";

        Map<String, Object> result = transformer.fromCsvFormat(csv);

        assertNotNull(result);
        assertEquals("2024-01-01", result.get("reportDate"));
        assertEquals("DAILY", result.get("reportType"));
        assertEquals("PAYU", result.get("institutionCode"));
        assertEquals(100, result.get("totalTransactions"));
    }

    @Test
    void testFromCsvFormatInvalid() {
        String csv = "invalid csv content";

        assertThrows(IllegalArgumentException.class, () -> transformer.fromCsvFormat(csv));
    }

    @Test
    void testCreateReportData() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        Map<String, Object> result = transformer.createReportData(null, "DAILY_CSV", date);

        assertNotNull(result);
        assertEquals("2024-01-15", result.get("reportDate"));
        assertEquals("DAILY_CSV", result.get("reportType"));
        assertEquals("PAYU", result.get("institutionCode"));
        assertEquals("DAILY", result.get("period"));
    }
}
