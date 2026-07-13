package id.payu.dispute.config;

import id.payu.dispute.DisputeServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigurationTest {

    @Test
    void jpaRepositoriesMustOnlyBeRegisteredOnce() {
        long registrations = java.util.stream.Stream
                .of(DisputeServiceApplication.class, JpaConfig.class)
                .filter(type -> type.isAnnotationPresent(EnableJpaRepositories.class))
                .count();

        assertThat(registrations).isEqualTo(1);
    }

    @Test
    void serviceMustUseSharedMaskedStructuredLogging() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("logback-spring.xml")) {
            assertThat(input).isNotNull();
            assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8))
                    .contains("logback-payu-base.xml");
        }
    }
}
