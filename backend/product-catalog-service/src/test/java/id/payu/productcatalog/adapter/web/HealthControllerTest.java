package id.payu.productcatalog.adapter.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.listener.ListenerContainerRegistry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Unit Tests")
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private ListenerContainerRegistry listenerRegistry;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Test
    @DisplayName("Should return UP when database is healthy and no kafka configured")
    void shouldReturnUpWhenDbHealthy() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(listenerRegistry.getListenerContainerIds()).thenReturn(new HashSet<>());

        HealthController controller = new HealthController(dataSource, listenerRegistry);
        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .containsEntry("status", "UP")
                .containsEntry("service", "product-catalog-service");
        assertThat(response.getBody()).containsKey("details");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) response.getBody().get("details");
        assertThat(details).containsEntry("database", "UP")
                .containsEntry("kafka", "NOT_CONFIGURED");
    }

    @Test
    @DisplayName("Should return DOWN when database is unavailable")
    void shouldReturnDownWhenDbUnhealthy() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));
        when(listenerRegistry.getListenerContainerIds()).thenReturn(new HashSet<>());

        HealthController controller = new HealthController(dataSource, listenerRegistry);
        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "DOWN");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) response.getBody().get("details");
        assertThat(details).containsEntry("database", "DOWN");
        assertThat(details).containsKey("database.error");
    }

    @Test
    @DisplayName("Should handle null ListenerContainerRegistry gracefully")
    void shouldHandleNullListenerRegistry() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        HealthController controller = new HealthController(dataSource, null);
        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getBody()).containsEntry("status", "UP");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) response.getBody().get("details");
        assertThat(details).containsEntry("kafka", "NOT_CONFIGURED");
    }
}
