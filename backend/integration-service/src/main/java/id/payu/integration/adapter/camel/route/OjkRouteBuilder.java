package id.payu.integration.adapter.camel.route;

import id.payu.integration.adapter.camel.transformer.OjkTransformer;
import id.payu.integration.adapter.camel.validator.OjkValidator;
import id.payu.integration.application.port.out.MessagePublisherPort;
import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageDirection;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;
import id.payu.integration.application.service.MessageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Camel routes for OJK regulatory reporting.
 * Handles daily and monthly report generation and submission.
 *
 * <p>ARCH-INTG-001: error events are published via
 * {@link MessagePublisherPort} (transactional outbox) — no direct
 * {@code kafka:} endpoints.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OjkRouteBuilder extends RouteBuilder {

    private static final String OJK_ERRORS_TOPIC = "payu.integration.ojk-errors.v1";

    private final OjkValidator ojkValidator;
    private final OjkTransformer ojkTransformer;
    private final MessageProcessingService messageProcessingService;
    private final MessagePublisherPort messagePublisherPort;

    @Value("${payu.integration.ojk.daily-report.enabled:true}")
    private boolean dailyReportEnabled;

    @Value("${payu.integration.ojk.monthly-report.enabled:true}")
    private boolean monthlyReportEnabled;

    @Value("${payu.integration.ojk.upload-url:https://reporting.ojk.go.id/api/v1/upload}")
    private String ojkUploadUrl;

    @Override
    public void configure() throws Exception {

        // Error handler
        onException(Exception.class)
            .log(LoggingLevel.ERROR, "Error processing OJK report: ${exception.message}")
            .to("direct:ojk-error-handler")
            .handled(true);

        // Route: Daily CSV Report Generation (triggered by timer)
        from("timer:ojk-daily-report?period=86400000&delay=60000") // Daily, 1 min delay on startup
            .routeId("ojk-dsv-daily-report-route")
            .choice()
                .when(constant(dailyReportEnabled))
                    .log(LoggingLevel.INFO, "Generating OJK daily CSV report")
                    .setHeader("reportType", constant("DAILY_CSV"))
                    .process(exchange -> exchange.getIn().setHeader("reportDate", LocalDate.now().minusDays(1).toString()))
                    .to("direct:ojk-generate-csv-report")
                .otherwise()
                    .log(LoggingLevel.DEBUG, "Daily CSV report generation is disabled");

        // Route: Monthly XML Report Generation (triggered by cron - 1st of month at 1 AM)
        from("cron:ojk-monthly-report?schedule=0+0+1+1+*+?")
            .routeId("ojk-xml-monthly-report-route")
            .choice()
                .when(constant(monthlyReportEnabled))
                    .log(LoggingLevel.INFO, "Generating OJK monthly XML report")
                    .setHeader("reportType", constant("MONTHLY_XML"))
                    .process(exchange -> exchange.getIn().setHeader("reportDate", LocalDate.now().minusMonths(1).withDayOfMonth(1).toString()))
                    .to("direct:ojk-generate-xml-report")
                .otherwise()
                    .log(LoggingLevel.DEBUG, "Monthly XML report generation is disabled");

        // Route: Generate CSV Report
        from("direct:ojk-csv-report")
            .routeId("ojk-csv-report-route")
            .to("direct:ojk-generate-csv-report");

        from("direct:ojk-generate-csv-report")
            .routeId("ojk-generate-csv-route")
            .process(exchange -> {
                String reportType = exchange.getIn().getHeader("reportType", String.class);
                String reportDateStr = exchange.getIn().getHeader("reportDate", String.class);
                LocalDate reportDate = reportDateStr != null
                        ? LocalDate.parse(reportDateStr)
                        : LocalDate.now();

                // Create report data (in production, fetch from database)
                Map<String, Object> reportData = ojkTransformer.createReportData(null, reportType, reportDate);

                // Create integration message
                IntegrationMessage message = IntegrationMessage.builder()
                        .messageId(UUID.randomUUID().toString())
                        .type(MessageType.OJK_CSV)
                        .direction(MessageDirection.OUTBOUND)
                        .sourceSystem("PAYU_CORE")
                        .targetSystem("OJK_REPORTING")
                        .rawPayload(reportData.toString())
                        .businessReference("OJK_" + reportType + "_" + reportDate.format(DateTimeFormatter.BASIC_ISO_DATE))
                        .status(MessageStatus.TRANSFORMING)
                        .build();

                exchange.getIn().setBody(message);
                exchange.setProperty("reportData", reportData);
            })
            .bean(messageProcessingService, "createMessage")
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> reportData = exchange.getProperty("reportData", Map.class);

                OjkValidator.ValidationResult validation = ojkValidator.validateReportData(reportData);
                if (!validation.valid()) {
                    throw new IllegalArgumentException("Report validation failed: " + validation.getErrorMessage());
                }

                String csvContent = ojkTransformer.toCsvFormat(reportData);

                IntegrationMessage message = exchange.getIn().getBody(IntegrationMessage.class);
                message.setTransformedPayload(csvContent);

                // Validate generated CSV
                OjkValidator.ValidationResult csvValidation = ojkValidator.validateCsv(csvContent);
                if (!csvValidation.valid()) {
                    throw new IllegalArgumentException("CSV validation failed: " + csvValidation.getErrorMessage());
                }

                exchange.getIn().setBody(csvContent);
                exchange.getIn().setHeader(Exchange.FILE_NAME,
                        "OJK_Daily_" + reportData.get("reportDate") + ".csv");
            })
            .to("file:/tmp/ojk-reports?fileExist=Override")
            .process(exchange -> {
                String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                log.info("OJK CSV report saved: {}", fileName);
            })
            .setHeader(Exchange.CONTENT_TYPE, constant("text/csv"))
            .setHeader("Accept-Encoding", constant("identity"))
            .toD(ojkUploadUrl + "?throwExceptionOnFailure=true")
            .log(LoggingLevel.INFO, "OJK CSV report uploaded successfully");

        // Route: Generate XML Report
        from("direct:ojk-xml-report")
            .routeId("ojk-xml-report-route")
            .to("direct:ojk-generate-xml-report");

        from("direct:ojk-generate-xml-report")
            .routeId("ojk-generate-xml-route")
            .process(exchange -> {
                String reportType = exchange.getIn().getHeader("reportType", String.class);
                String reportDateStr = exchange.getIn().getHeader("reportDate", String.class);
                LocalDate reportDate = reportDateStr != null
                        ? LocalDate.parse(reportDateStr)
                        : LocalDate.now();

                // Create report data
                Map<String, Object> reportData = ojkTransformer.createReportData(null, reportType, reportDate);
                reportData.put("period", "MONTHLY");

                // Create integration message
                IntegrationMessage message = IntegrationMessage.builder()
                        .messageId(UUID.randomUUID().toString())
                        .type(MessageType.OJK_XML)
                        .direction(MessageDirection.OUTBOUND)
                        .sourceSystem("PAYU_CORE")
                        .targetSystem("OJK_REPORTING")
                        .rawPayload(reportData.toString())
                        .businessReference("OJK_" + reportType + "_" + reportDate.format(DateTimeFormatter.ofPattern("yyyyMM")))
                        .status(MessageStatus.TRANSFORMING)
                        .build();

                exchange.getIn().setBody(message);
                exchange.setProperty("reportData", reportData);
            })
            .bean(messageProcessingService, "createMessage")
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> reportData = exchange.getProperty("reportData", Map.class);

                OjkValidator.ValidationResult validation = ojkValidator.validateReportData(reportData);
                if (!validation.valid()) {
                    throw new IllegalArgumentException("Report validation failed: " + validation.getErrorMessage());
                }

                String xmlContent = ojkTransformer.toXmlFormat(reportData);

                IntegrationMessage message = exchange.getIn().getBody(IntegrationMessage.class);
                message.setTransformedPayload(xmlContent);

                // Validate generated XML
                OjkValidator.ValidationResult xmlValidation = ojkValidator.validateXml(xmlContent);
                if (!xmlValidation.valid()) {
                    throw new IllegalArgumentException("XML validation failed: " + xmlValidation.getErrorMessage());
                }

                exchange.getIn().setBody(xmlContent);
                exchange.getIn().setHeader(Exchange.FILE_NAME,
                        "OJK_Monthly_" + reportData.get("reportDate").toString().substring(0, 7) + ".xml");
            })
            .to("file:/tmp/ojk-reports?fileExist=Override")
            .process(exchange -> {
                String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                log.info("OJK XML report saved: {}", fileName);
            })
            .setHeader(Exchange.CONTENT_TYPE, constant("application/xml"))
            .setHeader("Accept-Encoding", constant("identity"))
            .toD(ojkUploadUrl + "?throwExceptionOnFailure=true")
            .log(LoggingLevel.INFO, "OJK XML report uploaded successfully");

        // Route: Error handler
        from("direct:ojk-error-handler")
            .routeId("ojk-error-handler-route")
            .process(exchange -> {
                Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                log.error("OJK report processing error: {}", exception.getMessage());

                // Send alert/notification via transactional outbox
                messagePublisherPort.publishEvent(
                        "IntegrationMessage",
                        exchange.getExchangeId(),
                        "OjkReportFailed",
                        Map.of(
                                "error", exception.getMessage() != null ? exception.getMessage() : exception.getClass().getName(),
                                "timestamp", LocalDate.now().toString(),
                                "service", "integration-service"),
                        OJK_ERRORS_TOPIC);
            });
    }
}
