package id.payu.fx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableScheduling
public class FxSchedulerConfig {

    @Bean
    public ScheduledExecutorService fxRateUpdateExecutor() {
        return Executors.newScheduledThreadPool(2);
    }
}
