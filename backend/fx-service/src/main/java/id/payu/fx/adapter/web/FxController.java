package id.payu.fx.adapter.web;

import id.payu.fx.application.service.FxConversionService;
import id.payu.fx.application.service.FxConversionNotFoundException;
import id.payu.fx.application.service.FxRateService;
import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.dto.ConvertCurrencyRequest;
import id.payu.fx.dto.FxConversionResponse;
import id.payu.fx.dto.FxRateResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/fx-api/v1")
public class FxController {

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
    public ResponseEntity<FxRateResponse> getCurrentRate(
            @PathVariable String fromCurrency,
            @PathVariable String toCurrency) {
        
        rateQueryCounter.increment();
        FxRate rate = fxRateService.getCurrentRate(fromCurrency, toCurrency);
        return ResponseEntity.ok(toResponse(rate));
    }

    @GetMapping("/rates")
    public ResponseEntity<List<FxRateResponse>> getAllRates() {
        List<FxRate> rates = fxRateService.getAllRates();
        List<FxRateResponse> responses = rates.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/conversions/estimate")
    public ResponseEntity<FxConversionResponse> estimateConversion(
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
        
        return ResponseEntity.ok(toResponse(conversion));
    }

    @PostMapping("/conversions")
    public ResponseEntity<FxConversionResponse> createConversion(
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
            
            return ResponseEntity.ok(toResponse(conversion));
        });
    }

    @GetMapping("/conversions/{conversionId}")
    public ResponseEntity<FxConversionResponse> getConversion(
            @PathVariable UUID conversionId) {
        
        FxConversion conversion = fxConversionService.getConversion(conversionId);
        return ResponseEntity.ok(toResponse(conversion));
    }

    @GetMapping("/conversions")
    public ResponseEntity<List<FxConversionResponse>> getConversions(
            @AuthenticationPrincipal Jwt jwt) {
        
        String accountId = jwt.getClaim("account_id");
        List<FxConversion> conversions = fxConversionService.getConversionsByAccount(accountId);
        
        List<FxConversionResponse> responses = conversions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/conversions/{conversionId}/reverse")
    public ResponseEntity<Void> reverseConversion(
            @PathVariable UUID conversionId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String accountId = jwt.getClaim("account_id");
        FxConversion conversion = fxConversionService.getConversion(conversionId);
        
        if (!conversion.getAccountId().equals(accountId)) {
            return ResponseEntity.status(403).build();
        }
        
        fxConversionService.reverseConversion(conversionId);
        return ResponseEntity.ok().build();
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
