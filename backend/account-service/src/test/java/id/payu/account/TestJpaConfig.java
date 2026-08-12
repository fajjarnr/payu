package id.payu.account;

import jakarta.persistence.EntityManagerFactory;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Test configuration that provides mock JPA/DataSource beans
 * so that @EnableJpaRepositories on AccountServiceApplication
 * does not fail during slice tests (@WebMvcTest etc.) or when
 * DataSource auto-configuration is excluded (VaultConfigurationTest).
 */
@TestConfiguration
public class TestJpaConfig {

    @Bean
    public EntityManagerFactory entityManagerFactory() {
        return Mockito.mock(EntityManagerFactory.class);
    }

    @Bean
    public DataSource dataSource() {
        return Mockito.mock(DataSource.class);
    }
}
