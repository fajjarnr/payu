package id.payu.account.health;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ListenerContainerRegistry;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: health indicator coverage.
 */
@DisplayName("Account health indicators")
class HealthIndicatorTest {

    private Connection connection() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.isValid(5)).thenReturn(true);
        when(conn.getMetaData()).thenReturn(mock(java.sql.DatabaseMetaData.class));
        return conn;
    }

    private RemoteCacheManager cacheManager(boolean reachable) {
        @SuppressWarnings("unchecked")
        RemoteCache<String, String> cache = mock(RemoteCache.class);
        when(cache.containsKey("__payu_health__")).thenReturn(false);
        org.infinispan.client.hotrod.configuration.Configuration config =
                mock(org.infinispan.client.hotrod.configuration.Configuration.class);
        when(config.remoteCaches()).thenReturn(java.util.Collections.emptyMap());
        RemoteCacheManager rcm = mock(RemoteCacheManager.class);
        when(rcm.getConfiguration()).thenReturn(config);
        doReturn(cache).when(rcm).getCache();
        return rcm;
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, Object> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    private DeepHealthIndicator deepHealth(DataSource ds, RemoteCacheManager rcm) {
        return new DeepHealthIndicator(ds, rcm, kafkaTemplate(), mock(ListenerContainerRegistry.class));
    }

    @Test
    void livenessIsUpWithJvmDetails() {
        LivenessHealthIndicator indicator = new LivenessHealthIndicator();
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKeys("heapUsed", "threadCount", "uptime");
    }

    @Test
    void deepHealthIsDownWhenDatabaseFails() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new java.sql.SQLException("db down"));

        Health health = deepHealth(ds, cacheManager(true)).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("database")).isNotNull();
    }

    @Test
    void deepHealthIsDownWhenDataGridFails() throws Exception {
        Connection conn = connection();
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenReturn(conn);
        RemoteCacheManager rcm = mock(RemoteCacheManager.class);
        when(rcm.getConfiguration()).thenThrow(new RuntimeException("hotrod down"));

        Health health = deepHealth(ds, rcm).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void readinessIsOutOfServiceWhenDatabaseDown() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new java.sql.SQLException("db down"));


        ReadinessHealthIndicator indicator = new ReadinessHealthIndicator(
                ds, cacheManager(true), mock(ListenerContainerRegistry.class));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsKey("database");
    }
}
