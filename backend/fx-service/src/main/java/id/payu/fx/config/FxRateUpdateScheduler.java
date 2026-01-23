package id.payu.fx.config;

import id.payu.fx.application.service.FxRateService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FxRateUpdateScheduler {

    private final FxRateService fxRateService;
    private final MeterRegistry meterRegistry;
    private final boolean enabled;

    public FxRateUpdateScheduler(FxRateService fxRateService, 
                                MeterRegistry meterRegistry,
                                @Value("${fx.scheduler.enabled:true}") boolean enabled) {
        this.fxRateService = fxRateService;
        this.meterRegistry = meterRegistry;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${fx.scheduler.update-rates-cron:0 */15 * * * *}")
    public void updateFxRates() {
        if (!enabled) {
            return;
        }
        
        try {
            fxRateService.updateRates();
            meterRegistry.counter("fx.rate.update.success").increment();
        } catch (Exception e) {
            meterRegistry.counter("fx.rate.update.failed").increment();
            throw e;
        }
    }
}
