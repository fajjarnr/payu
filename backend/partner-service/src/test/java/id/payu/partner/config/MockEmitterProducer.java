package id.payu.partner.config;

import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Alternative;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * Mock Emitter producer for tests.
 *
 * <p>This producer creates a no-op Emitter to prevent Kafka connector activation
 * during tests. Without this, Quarkus would try to create the Kafka connector
 * which requires Hibernate Reactive for checkpoint state store.</p>
 *
 * <p>This is an alternative bean that takes precedence during test execution.</p>
 */
@ApplicationScoped
@Alternative
public class MockEmitterProducer {

    @SuppressWarnings("unchecked")
    @Produces
    @ApplicationScoped
    @Alternative
    public MutinyEmitter<String> createMockEmitter() {
        MutinyEmitter<String> mockEmitter = Mockito.mock(MutinyEmitter.class);
        when(mockEmitter.hasRequests()).thenReturn(true);
        when(mockEmitter.isCancelled()).thenReturn(false);
        return mockEmitter;
    }
}
