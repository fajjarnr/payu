package id.payu.account;

import jakarta.persistence.EntityManagerFactory;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration that provides a mock EntityManagerFactory
 * so that @EnableJpaRepositories on AccountServiceApplication
 * does not fail during slice tests (@WebMvcTest etc.).
 */
@TestConfiguration
public class TestJpaConfig {

    @Bean
    public EntityManagerFactory entityManagerFactory() {
        return Mockito.mock(EntityManagerFactory.class);
    }
}
