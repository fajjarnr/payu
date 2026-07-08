package id.payu.integration.adapter.camel.transformer;

import id.payu.integration.domain.model.IntegrationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Transformer for OJK regulatory reporting formats.
 * Handles CSV and XML transformations.
 */
@Component
@Slf4j
public class OjkTransformer {

    private static final String CSV_HEADER = "ReportDate,ReportType,InstitutionCode,TotalTransactions,TotalAmount,Currency";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Transform internal data to OJK CSV format.
     */
    public String toCsvFormat(Map<String, Object> reportData) {
        log.debug("Transforming to OJK CSV format");

        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER).append("\n");

        String reportDate = (String) reportData.get("reportDate");
        String reportType = (String) reportData.get("reportType");
        String institutionCode = (String) reportData.getOrDefault("institutionCode", "PAYU");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) reportData.get("transactions");

        if (transactions != null) {
            for (Map<String, Object> transaction : transactions) {
                csv.append(reportDate).append(",")
                        .append(reportType).append(",")
                        .append(institutionCode).append(",")
                        .append(transaction.getOrDefault("count", 0)).append(",")
                        .append(transaction.getOrDefault("amount", "0.00")).append(",")
                        .append(transaction.getOrDefault("currency", "IDR"))
                        .append("\n");
            }
        } else {
            // Summary row if no transaction details
            csv.append(reportDate).append(",")
                    .append(reportType).append(",")
                    .append(institutionCode).append(",")
                    .append(reportData.getOrDefault("totalTransactions", 0)).append(",")
                    .append(reportData.getOrDefault("totalAmount", "0.00")).append(",")
                    .append(reportData.getOrDefault("currency", "IDR"))
                    .append("\n");
        }

        return csv.toString();
    }

    /**
     * Transform internal data to OJK XML format.
     */
    public String toXmlFormat(Map<String, Object> reportData) {
        log.debug("Transforming to OJK XML format");

        String reportDate = (String) reportData.get("reportDate");
        String reportType = (String) reportData.get("reportType");
        String institutionCode = (String) reportData.getOrDefault("institutionCode", "PAYU");
        String period = (String) reportData.getOrDefault("period", "DAILY");

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<OJKReport xmlns=\"http://www.ojk.go.id/reporting/v1\">\n");
        xml.append("  <Header>\n");
        xml.append("    <InstitutionCode>").append(escapeXml(institutionCode)).append("</InstitutionCode>\n");
        xml.append("    <ReportType>").append(escapeXml(reportType)).append("</ReportType>\n");
        xml.append("    <ReportDate>").append(reportDate).append("</ReportDate>\n");
        xml.append("    <Period>").append(period).append("</Period>\n");
        xml.append("    <GeneratedAt>").append(LocalDate.now().format(DATE_FORMATTER)).append("</GeneratedAt>\n");
        xml.append("  </Header>\n");

        xml.append("  <Summary>\n");
        xml.append("    <TotalTransactions>").append(reportData.getOrDefault("totalTransactions", 0)).append("</TotalTransactions>\n");
        xml.append("    <TotalAmount currency=\"").append(reportData.getOrDefault("currency", "IDR")).append("\">");
        xml.append(reportData.getOrDefault("totalAmount", "0.00")).append("</TotalAmount>\n");
        xml.append("  </Summary>\n");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) reportData.get("details");
        if (details != null && !details.isEmpty()) {
            xml.append("  <Details>\n");
            for (Map<String, Object> detail : details) {
                xml.append("    <Transaction>\n");
                xml.append("      <TransactionId>").append(escapeXml((String) detail.get("transactionId"))).append("</TransactionId>\n");
                xml.append("      <TransactionDate>").append(detail.get("transactionDate")).append("</TransactionDate>\n");
                xml.append("      <Amount currency=\"").append(detail.getOrDefault("currency", "IDR")).append("\">");
                xml.append(detail.getOrDefault("amount", "0.00")).append("</Amount>\n");
                xml.append("      <Type>").append(escapeXml((String) detail.get("type"))).append("</Type>\n");
                xml.append("      <Status>").append(detail.getOrDefault("status", "COMPLETED")).append("</Status>\n");
                xml.append("    </Transaction>\n");
            }
            xml.append("  </Details>\n");
        }

        xml.append("</OJKReport>");

        return xml.toString();
    }

    /**
     * Parse OJK CSV format to internal data.
     */
    public Map<String, Object> fromCsvFormat(String csvContent) {
        log.debug("Parsing OJK CSV format");

        String[] lines = csvContent.split("\n");
        if (lines.length < 2) {
            throw new IllegalArgumentException("Invalid CSV format: insufficient lines");
        }

        // Skip header, parse first data row
        String[] fields = lines[1].split(",");

        return Map.of(
                "reportDate", fields[0],
                "reportType", fields[1],
                "institutionCode", fields[2],
                "totalTransactions", Integer.parseInt(fields[3]),
                "totalAmount", fields[4],
                "currency", fields[5]
        );
    }

    /**
     * Create report data structure from message.
     */
    public Map<String, Object> createReportData(IntegrationMessage message, String reportType, LocalDate date) {
        return new java.util.HashMap<>(Map.of(
                "reportDate", date.format(DATE_FORMATTER),
                "reportType", reportType,
                "institutionCode", "PAYU",
                "period", reportType.toUpperCase().contains("MONTHLY") ? "MONTHLY" : "DAILY",
                "totalTransactions", 0,
                "totalAmount", "0.00",
                "currency", "IDR"
        ));
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
