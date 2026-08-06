package id.payu.datasource.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ConfigurationPropertiesAutoConfiguration.class,
            DataSourceAutoConfiguration.class
        ))
        .withPropertyValues(
            "spring.profiles.active=local",
            "spring.datasource.primary.hikari.jdbc-url=jdbc:postgresql://localhost:5432/payu_account",
            "spring.datasource.primary.hikari.username=payu",
            "spring.datasource.primary.hikari.password=payu_secret"
        );

    @Test
    void bindsPrimaryDatasourceFromServiceProfileContract() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            HikariDataSource dataSource = context.getBean("primaryDataSource", HikariDataSource.class);
            assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/payu_account");
            assertThat(dataSource.getUsername()).isEqualTo("payu");
        });
    }

    @Test
    void doesNotCreateEmptyPrimaryWhenOnlyStandardDatasourceIsConfigured() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withPropertyValues(
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/payu_auth",
                "spring.datasource.username=payu",
                "spring.datasource.password=payu_secret"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean("primaryDataSource");
            });
    }
}
