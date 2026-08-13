package id.payu.account.health;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
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

    private DataSource dataSource;
    private RemoteCacheManager rcm;
    private ListenerContainerRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(mock(java.sql.Statement.class));
        when(conn.isValid(5)).thenReturn(true);
        java.sql.DatabaseMetaData meta = mock(java.sql.DatabaseMetaData.class);
        when(meta.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(meta.getDatabaseProductVersion()).thenReturn("16");
        when(conn.getMetaData()).thenReturn(meta);
        when(dataSource.getConnection()).thenReturn(conn);

        @SuppressWarnings("unchecked")
        RemoteCache<String, String> cache = mock(RemoteCache.class);
        when(cache.containsKey("__payu_health__")).thenReturn(false);
        org.infinispan.client.hotrod.configuration.Configuration config =
                mock(org.infinispan.client.hotrod.configuration.Configuration.class);
        when(config.remoteCaches()).thenReturn(java.util.Collections.emptyMap());
        rcm = mock(RemoteCacheManager.class);
        when(rcm.getConfiguration()).thenReturn(config);
        doReturn(cache).when(rcm).getCache();

        registry = mock(ListenerContainerRegistry.class);
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> kt = mock(KafkaTemplate.class);
        org.springframework.kafka.core.ProducerFactory<String, Object> pf =
                mock(org.springframework.kafka.core.ProducerFactory.class);
        when(pf.getConfigurationProperties()).thenReturn(java.util.Collections.emptyMap());
        when(kt.getProducerFactory()).thenReturn(pf);
        return kt;
    }

    private DeepHealthIndicator deepHealth(DataSource ds, RemoteCacheManager mgr) {
        return new DeepHealthIndicator(ds, mgr, kafkaTemplate(), registry);
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
        DataSource bad = mock(DataSource.class);
        when(bad.getConnection()).thenThrow(new java.sql.SQLException("db down"));

        Health health = deepHealth(bad, rcm).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("database")).isNotNull();
    }

    @Test
    void deepHealthIsDownWhenDataGridFails() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(mock(java.sql.Statement.class));
        when(conn.isValid(5)).thenReturn(true);
        java.sql.DatabaseMetaData meta = mock(java.sql.DatabaseMetaData.class);
        when(meta.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(meta.getDatabaseProductVersion()).thenReturn("16");
        when(conn.getMetaData()).thenReturn(meta);
        when(dataSource.getConnection()).thenReturn(conn);
        RemoteCacheManager bad = mock(RemoteCacheManager.class);
        when(bad.getConfiguration()).thenThrow(new RuntimeException("hotrod down"));

        Health health = deepHealth(dataSource, bad).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void readinessIsOutOfServiceWhenDatabaseDown() throws Exception {
        DataSource bad = mock(DataSource.class);
        when(bad.getConnection()).thenThrow(new java.sql.SQLException("db down"));

        ReadinessHealthIndicator indicator = new ReadinessHealthIndicator(
                bad, rcm, registry);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsKey("database");
    }

    @Test
    void readinessOutOfServiceWhenDataGridNotConfigured() {
        when(registry.getListenerContainerIds()).thenReturn(java.util.Set.of());

        ReadinessHealthIndicator indicator = new ReadinessHealthIndicator(dataSource, null, registry);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
    }

    @Test
    void readinessOutOfServiceWhenKafkaListenerNotRunning() {
        when(registry.getListenerContainerIds()).thenReturn(java.util.Set.of("listener-1"));
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        when(container.isRunning()).thenReturn(false);
        when(registry.getListenerContainer("listener-1")).thenReturn(container);

        ReadinessHealthIndicator indicator = new ReadinessHealthIndicator(dataSource, null, registry);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
    }

    @Test
    void dependencyHealthMapsDeepStatusUp() {
        when(registry.getListenerContainerIds()).thenReturn(java.util.Set.of());
        ApplicationAvailability availability = new ApplicationAvailability() {
            @Override
            public <S extends org.springframework.boot.availability.AvailabilityState> S getState(
                    Class<S> stateType, S defaultState) {
                return defaultState;
            }

            @Override
            public <S extends org.springframework.boot.availability.AvailabilityState> S getState(
                    Class<S> stateType) {
                return null;
            }

            @Override
            public <S extends org.springframework.boot.availability.AvailabilityState>
                    org.springframework.boot.availability.AvailabilityChangeEvent<S>
                    getLastChangeEvent(Class<S> stateType) {
                return null;
            }
        };

        DependencyHealthIndicator indicator = new DependencyHealthIndicator(availability, deepHealth(dataSource, rcm));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKeys("liveness", "readiness", "deepHealth", "dependencies");
    }

    @Test
    void dependencyHealthMarksUnhealthyWhenDeepDown() throws Exception {
        DataSource bad = mock(DataSource.class);
        when(bad.getConnection()).thenThrow(new java.sql.SQLException("db down"));

        DependencyHealthIndicator indicator = new DependencyHealthIndicator(
                new ApplicationAvailability() {
                    @Override
                    public <S extends org.springframework.boot.availability.AvailabilityState> S getState(
                            Class<S> stateType, S defaultState) { return defaultState; }
                    @Override
                    public <S extends org.springframework.boot.availability.AvailabilityState> S getState(
                            Class<S> stateType) { return null; }
                    @Override
                    public <S extends org.springframework.boot.availability.AvailabilityState>
                            org.springframework.boot.availability.AvailabilityChangeEvent<S>
                            getLastChangeEvent(Class<S> stateType) { return null; }
                }, deepHealth(bad, rcm));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }
}
