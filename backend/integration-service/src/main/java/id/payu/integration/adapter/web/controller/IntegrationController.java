package id.payu.integration.adapter.web.controller;

import id.payu.integration.adapter.web.dto.*;
import id.payu.integration.application.port.in.IntegrationUseCase;
import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.integration.domain.model.MessageStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for integration operations.
 * Provides APIs for SWIFT, OJK, and SOAP integrations.
 */
@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Integration", description = "Legacy system integration APIs for SWIFT, OJK reporting, and SOAP")
public class IntegrationController {

    private final IntegrationUseCase integrationUseCase;

    @PostMapping("/swift/process")
    @Operation(summary = "Process SWIFT message",
            description = "Process an incoming SWIFT MT message (MT103, MT202, MT940)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message processed successfully",
                content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid SWIFT message format"),
        @ApiResponse(responseCode = "500", description = "Processing error")
    })
    public ResponseEntity<Map<String, String>> processSwift(
            @Valid @RequestBody SwiftMessageRequest request) {
        log.info("Processing SWIFT message of type: {}", request.getMessageType());

        String messageId = integrationUseCase.processSwiftMessage(
                request.getSwiftMessage(),
                request.getMessageType()
        );

        return ResponseEntity.ok(Map.of(
                "messageId", messageId,
                "status", "PROCESSING",
                "message", "SWIFT message accepted for processing"
        ));
    }

    @PostMapping("/ojk/generate-report")
    @Operation(summary = "Generate OJK regulatory report",
            description = "Generate and submit OJK regulatory report (CSV or XML format)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid report parameters"),
        @ApiResponse(responseCode = "500", description = "Report generation error")
    })
    public ResponseEntity<Map<String, String>> generateOjkReport(
            @Valid @RequestBody OjkReportRequest request) {
        log.info("Generating OJK report of type: {} for date: {}",
                request.getReportType(), request.getReportDate());

        String messageId = integrationUseCase.generateOjkReport(
                request.getReportType(),
                request.getReportDate()
        );

        return ResponseEntity.ok(Map.of(
                "messageId", messageId,
                "status", "GENERATING",
                "message", "OJK report generation initiated"
        ));
    }

    @PostMapping("/soap/send")
    @Operation(summary = "Send SOAP request",
            description = "Send SOAP request to legacy system endpoint")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SOAP request sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid SOAP request"),
        @ApiResponse(responseCode = "500", description = "SOAP communication error")
    })
    public ResponseEntity<Map<String, String>> sendSoap(
            @Valid @RequestBody SoapRequest request) {
        log.info("Sending SOAP request to: {} operation: {}",
                request.getEndpoint(), request.getOperation());

        String response = integrationUseCase.sendSoapRequest(
                request.getEndpoint(),
                request.getOperation(),
                request.getPayload()
        );

        return ResponseEntity.ok(Map.of(
                "response", response,
                "status", "SUCCESS"
        ));
    }

    @PostMapping("/http/send")
    @Operation(summary = "Send HTTP request",
            description = "Send generic HTTP request to external system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "HTTP request sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid HTTP request"),
        @ApiResponse(responseCode = "500", description = "HTTP communication error")
    })
    public ResponseEntity<Map<String, String>> sendHttp(
            @Valid @RequestBody HttpRequest request) {
        log.info("Sending HTTP {} request to: {}", request.getMethod(), request.getUrl());

        String response = integrationUseCase.sendHttpRequest(
                request.getUrl(),
                request.getMethod(),
                request.getHeaders(),
                request.getBody()
        );

        return ResponseEntity.ok(Map.of(
                "response", response,
                "status", "SUCCESS"
        ));
    }

    @GetMapping("/messages/{messageId}/status")
    @Operation(summary = "Get message processing status",
            description = "Retrieve the current status of an integration message")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<IntegrationMessageResponse> getMessageStatus(
            @Parameter(description = "Message ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String messageId) {
        log.debug("Getting status for message: {}", messageId);

        IntegrationMessage message = integrationUseCase.getMessageStatus(messageId);
        return ResponseEntity.ok(IntegrationMessageResponse.from(message));
    }

    @GetMapping("/messages")
    @Operation(summary = "Get messages by status",
            description = "List integration messages filtered by status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    })
    public ResponseEntity<List<IntegrationMessageResponse>> getMessagesByStatus(
            @Parameter(description = "Message status filter", example = "FAILED")
            @RequestParam MessageStatus status) {
        log.debug("Getting messages with status: {}", status);

        List<IntegrationMessage> messages = integrationUseCase.getMessagesByStatus(status);
        List<IntegrationMessageResponse> responses = messages.stream()
                .map(IntegrationMessageResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/messages/{messageId}/retry")
    @Operation(summary = "Retry failed message",
            description = "Queue a failed message for retry processing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message queued for retry"),
        @ApiResponse(responseCode = "400", description = "Message cannot be retried"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<Map<String, String>> retryMessage(
            @Parameter(description = "Message ID", required = true)
            @PathVariable String messageId) {
        log.info("Retrying message: {}", messageId);

        boolean queued = integrationUseCase.retryMessage(messageId);

        if (queued) {
            return ResponseEntity.ok(Map.of(
                    "messageId", messageId,
                    "status", "RETRY_QUEUED",
                    "message", "Message queued for retry"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "messageId", messageId,
                    "status", "CANNOT_RETRY",
                    "message", "Message has exceeded maximum retry attempts"
            ));
        }
    }

    @PostMapping("/messages/{messageId}/cancel")
    @Operation(summary = "Cancel pending message",
            description = "Cancel a message that has not been processed yet")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message cancelled"),
        @ApiResponse(responseCode = "400", description = "Message cannot be cancelled"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<Map<String, String>> cancelMessage(
            @Parameter(description = "Message ID", required = true)
            @PathVariable String messageId) {
        log.info("Cancelling message: {}", messageId);

        integrationUseCase.cancelMessage(messageId);

        return ResponseEntity.ok(Map.of(
                "messageId", messageId,
                "status", "CANCELLED",
                "message", "Message processing cancelled"
        ));
    }
}
