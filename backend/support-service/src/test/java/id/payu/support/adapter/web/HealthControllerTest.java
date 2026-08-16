package id.payu.support.adapter.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Test
    void reportsUpWhenDatabaseReachable() throws Exception {
        Connection conn = org.mockito.Mockito.mock(Connection.class);
        Statement stmt = org.mockito.Mockito.mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        var response = new HealthController(dataSource).health();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsEntry("service", "support-service");
        assertThat((java.util.Map<String, Object>) response.getBody().get("details"))
                .containsEntry("database", "UP");
    }

    @Test
    void reportsDownWhenDatabaseFails() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("db down"));

        var response = new HealthController(dataSource).health();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "DOWN");
        java.util.Map<String, Object> details =
                (java.util.Map<String, Object>) response.getBody().get("details");
        assertThat(details).containsEntry("database", "DOWN");
        assertThat((String) details.get("database.error")).contains("db down");
    }
}
