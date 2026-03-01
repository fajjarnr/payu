package id.payu.integration.adapter.camel.validator;

import id.payu.integration.domain.model.IntegrationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for OJK regulatory report formats.
 * Validates CSV and XML report structures.
 */
@Component
@Slf4j
public class OjkValidator {

    private static final Pattern INSTITUTION_CODE_PATTERN = Pattern.compile("^[A-Z]{4}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Validate OJK CSV report format.
     */
    public ValidationResult validateCsv(String csvContent) {
        log.debug("Validating OJK CSV report");

        List<String> errors = new ArrayList<>();

        if (csvContent == null || csvContent.trim().isEmpty()) {
            errors.add("CSV content is empty");
            return new ValidationResult(false, errors);
        }

        String[] lines = csvContent.split("\n");
        if (lines.length < 1) {
            errors.add("CSV has no content");
            return new ValidationResult(false, errors);
        }

        // Validate header
        String header = lines[0].trim();
        if (!header.contains("ReportDate") || !header.contains("ReportType") || !header.contains("InstitutionCode")) {
            errors.add("CSV header missing required columns");
        }

        // Validate data rows
        if (lines.length < 2) {
            errors.add("CSV has no data rows");
        } else {
            for (int i = 1; i < lines.length; i++) {
                validateCsvRow(lines[i], i + 1, errors);
            }
        }

        boolean valid = errors.isEmpty();
        if (valid) {
            log.debug("OJK CSV report is valid");
        } else {
            log.warn("OJK CSV validation failed: {}", errors);
        }

        return new ValidationResult(valid, errors);
    }

    /**
     * Validate OJK XML report format.
     */
    public ValidationResult validateXml(String xmlContent) {
        log.debug("Validating OJK XML report");

        List<String> errors = new ArrayList<>();

        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            errors.add("XML content is empty");
            return new ValidationResult(false, errors);
        }

        // Basic XML structure validation
        if (!xmlContent.trim().startsWith("<?xml")) {
            errors.add("XML declaration missing");
        }

        if (!xmlContent.contains("<OJKReport") || !xmlContent.contains("</OJKReport>")) {
            errors.add("Root element OJKReport missing");
        }

        // Validate required sections
        if (!xmlContent.contains("<Header>") || !xmlContent.contains("</Header>")) {
            errors.add("Header section missing");
        }

        if (!xmlContent.contains("<InstitutionCode>")) {
            errors.add("InstitutionCode element missing");
        }

        if (!xmlContent.contains("<ReportType>")) {
            errors.add("ReportType element missing");
        }

        if (!xmlContent.contains("<ReportDate>")) {
            errors.add("ReportDate element missing");
        }

        // Validate well-formedness
        try {
            javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(xmlContent)));
        } catch (Exception e) {
            errors.add("XML is not well-formed: " + e.getMessage());
        }

        boolean valid = errors.isEmpty();
        if (valid) {
            log.debug("OJK XML report is valid");
        } else {
            log.warn("OJK XML validation failed: {}", errors);
        }

        return new ValidationResult(valid, errors);
    }

    /**
     * Validate report data before transformation.
     */
    public ValidationResult validateReportData(java.util.Map<String, Object> reportData) {
        log.debug("Validating OJK report data");

        List<String> errors = new ArrayList<>();

        // Validate report date
        String reportDate = (String) reportData.get("reportDate");
        if (reportDate == null || reportDate.isEmpty()) {
            errors.add("Report date is required");
        } else {
            try {
                LocalDate.parse(reportDate, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                errors.add("Invalid report date format: " + reportDate);
            }
        }

        // Validate report type
        String reportType = (String) reportData.get("reportType");
        if (reportType == null || reportType.isEmpty()) {
            errors.add("Report type is required");
        }

        // Validate institution code
        String institutionCode = (String) reportData.get("institutionCode");
        if (institutionCode == null || institutionCode.isEmpty()) {
            errors.add("Institution code is required");
        } else if (!INSTITUTION_CODE_PATTERN.matcher(institutionCode).matches()) {
            errors.add("Invalid institution code format: " + institutionCode);
        }

        // Validate amounts
        Object totalAmount = reportData.get("totalAmount");
        if (totalAmount != null) {
            try {
                new BigDecimal(totalAmount.toString());
            } catch (NumberFormatException e) {
                errors.add("Invalid total amount: " + totalAmount);
            }
        }

        boolean valid = errors.isEmpty();
        if (valid) {
            log.debug("OJK report data is valid");
        } else {
            log.warn("OJK report data validation failed: {}", errors);
        }

        return new ValidationResult(valid, errors);
    }

    private void validateCsvRow(String row, int lineNumber, List<String> errors) {
        if (row.trim().isEmpty()) {
            return; // Skip empty lines
        }

        String[] fields = row.split(",");
        if (fields.length < 6) {
            errors.add("Line " + lineNumber + ": Insufficient fields (expected 6, got " + fields.length + ")");
            return;
        }

        // Validate date
        try {
            LocalDate.parse(fields[0].trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            errors.add("Line " + lineNumber + ": Invalid date format: " + fields[0]);
        }

        // Validate transaction count
        try {
            Integer.parseInt(fields[3].trim());
        } catch (NumberFormatException e) {
            errors.add("Line " + lineNumber + ": Invalid transaction count: " + fields[3]);
        }

        // Validate amount
        try {
            new BigDecimal(fields[4].trim());
        } catch (NumberFormatException e) {
            errors.add("Line " + lineNumber + ": Invalid amount: " + fields[4]);
        }

        // Validate currency
        String currency = fields[5].trim();
        if (!currency.matches("[A-Z]{3}")) {
            errors.add("Line " + lineNumber + ": Invalid currency code: " + currency);
        }
    }

    /**
     * Result of validation.
     */
    public record ValidationResult(boolean valid, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
