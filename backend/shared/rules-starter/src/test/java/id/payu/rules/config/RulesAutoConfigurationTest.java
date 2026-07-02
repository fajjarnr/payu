package id.payu.rules.config;

import id.payu.rules.service.RulesEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RulesAutoConfigurationTest")
class RulesAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RulesAutoConfiguration.class));

    @Test
    @DisplayName("should auto-configure RulesEngineService bean")
    void autoConfiguresRulesEngineService() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RulesEngineService.class);
        });
    }
}
