package id.payu.fx.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.fx.application.service.FxConversionService;
import id.payu.fx.application.service.FxRateService;
import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.dto.ConvertCurrencyRequest;
import id.payu.fx.dto.FxConversionResponse;
import id.payu.fx.dto.FxRateResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1")
@Tag(name = "Foreign Exchange", description = "FX rate queries and currency conversion APIs")
@SecurityRequirement(name = "bearerAuth")
public class FxController extends BaseController {

    private final FxRateService fxRateService;
    private final FxConversionService fxConversionService;
    private final Counter rateQueryCounter;
    private final Counter conversionCounter;
    private final Timer conversionTimer;

    public FxController(FxRateService fxRateService,
                       FxConversionService fxConversionService,
                       MeterRegistry meterRegistry) {
        this.fxRateService = fxRateService;
        this.fxConversionService = fxConversionService;
        this.rateQueryCounter = Counter.builder("fx.rate.queries")
                .description("Number of FX rate queries")
                .register(meterRegistry);
        this.conversionCounter = Counter.builder("fx.conversions")
                .description("Number of FX conversions")
                .register(meterRegistry);
        this.conversionTimer = Timer.builder("fx.conversion.duration")
                .description("FX conversion duration")
                .register(meterRegistry);
    }

    @GetMapping("/rates/{fromCurrency}/{toCurrency}")
    @Operation(summary = "Get current FX rate", description = "Retrieve the current exchange rate between two currencies")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FX rate retrieved successfully",
            content = @Content(schema = @Schema(implementation = FxRateResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FX rate not found for currency pair")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<FxRateResponse>> getCurrentRate(
            @Parameter(description = "Source currency code (e.g., USD)", required = true) @PathVariable String fromCurrency,
            @Parameter(description = "Target currency code (e.g., IDR)", required = true) @PathVariable String toCurrency) {

        rateQueryCounter.increment();
        FxRate rate = fxRateService.getCurrentRate(fromCurrency, toCurrency);
        return ok(toResponse(rate));
    }

    @GetMapping("/rates")
    @Operation(summary = "Get all FX rates", description = "Retrieve all available exchange rates")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FX rates retrieved successfully",
            content = @Content(schema = @Schema(implementation = FxRateResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<List<FxRateResponse>>> getAllRates() {
        List<FxRate> rates = fxRateService.getAllRates();
        List<FxRateResponse> responses = rates.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ok(responses);
    }

    @PostMapping("/conversions/estimate")
    @Operation(summary = "Estimate conversion", description = "Get a conversion estimate without executing the transaction")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversion estimate retrieved successfully",
            content = @Content(schema = @Schema(implementation = FxConversionResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FX rate not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<FxConversionResponse>> estimateConversion(
            @Valid @RequestBody ConvertCurrencyRequest request) {

        rateQueryCounter.increment();
        FxConversion conversion = fxConversionService.createConversion(
                FxConversion.builder()
                        .id(UUID.randomUUID())
                        .accountId("estimate")
                        .fromCurrency(request.getFromCurrency())
                        .toCurrency(request.getToCurrency())
                        .fromAmount(request.getAmount())
                        .build());

        return ok(toResponse(conversion));
    }

    @PostMapping("/conversions")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.TRANSFER,
            entityType = "FxConversion",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(summary = "Create conversion", description = "Execute a currency conversion transaction")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Conversion executed successfully",
            content = @Content(schema = @Schema(implementation = FxConversionResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - insufficient balance or validation error")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FX rate not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<FxConversionResponse>> createConversion(
            @Valid @RequestBody ConvertCurrencyRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        conversionCounter.increment();

        return conversionTimer.record(() -> {
            String accountId = jwt.getClaim("account_id");

            FxConversion conversion = fxConversionService.createConversion(
                    FxConversion.builder()
                            .id(UUID.randomUUID())
                            .accountId(accountId)
                            .fromCurrency(request.getFromCurrency())
                            .toCurrency(request.getToCurrency())
                            .fromAmount(request.getAmount())
                            .build());

            conversion.markCompleted();

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{conversionId}")
                    .buildAndExpand(conversion.getId())
                    .toUri();

            return created(toResponse(conversion), location.toString());
        });
    }

    @GetMapping("/conversions/{conversionId}")
    @Operation(summary = "Get conversion by ID", description = "Retrieve conversion transaction details")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversion found",
            content = @Content(schema = @Schema(implementation = FxConversionResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversion not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<FxConversionResponse>> getConversion(
            @Parameter(description = "Conversion ID", required = true) @PathVariable UUID conversionId) {

        FxConversion conversion = fxConversionService.getConversion(conversionId);
        return ok(toResponse(conversion));
    }

    @GetMapping("/conversions")
    @Operation(summary = "Get user conversions", description = "Retrieve all conversion transactions for the authenticated user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversions retrieved successfully",
            content = @Content(schema = @Schema(implementation = FxConversionResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<List<FxConversionResponse>>> getConversions(
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        List<FxConversion> conversions = fxConversionService.getConversionsByAccount(accountId);

        List<FxConversionResponse> responses = conversions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ok(responses);
    }

    @PostMapping("/conversions/{conversionId}/reverse")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
            entityType = "FxConversion",
            maskData = true,
            level = AuditLevel.WARN
    )
    @Operation(summary = "Reverse conversion", description = "Reverse a previously executed currency conversion")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversion reversed successfully",
            content = @Content(schema = @Schema(implementation = FxConversionResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - cannot reverse another user's conversion")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversion not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<FxConversionResponse>> reverseConversion(
            @Parameter(description = "Conversion ID", required = true) @PathVariable UUID conversionId,
            @AuthenticationPrincipal Jwt jwt) {

        String accountId = jwt.getClaim("account_id");
        FxConversion conversion = fxConversionService.getConversion(conversionId);

        if (!conversion.getAccountId().equals(accountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FX_403", "Cannot reverse another user's conversion"));
        }

        fxConversionService.reverseConversion(conversionId);
        FxConversion updatedConversion = fxConversionService.getConversion(conversionId);
        return ok(toResponse(updatedConversion));
    }

    private FxRateResponse toResponse(FxRate rate) {
        FxRateResponse response = new FxRateResponse();
        response.setId(rate.getId());
        response.setFromCurrency(rate.getFromCurrency());
        response.setToCurrency(rate.getToCurrency());
        response.setRate(rate.getRate());
        response.setInverseRate(rate.getInverseRate());
        response.setValidFrom(rate.getValidFrom());
        response.setValidUntil(rate.getValidUntil());
        return response;
    }

    private FxConversionResponse toResponse(FxConversion conversion) {
        FxConversionResponse response = new FxConversionResponse();
        response.setId(conversion.getId());
        response.setAccountId(conversion.getAccountId());
        response.setFromCurrency(conversion.getFromCurrency());
        response.setToCurrency(conversion.getToCurrency());
        response.setFromAmount(conversion.getFromAmount());
        response.setToAmount(conversion.getToAmount());
        response.setExchangeRate(conversion.getExchangeRate());
        response.setFee(conversion.getFee());
        response.setEffectiveAmount(conversion.getEffectiveAmount());
        response.setConversionDate(conversion.getConversionDate());
        response.setStatus(conversion.getStatus().name());
        return response;
    }
}
