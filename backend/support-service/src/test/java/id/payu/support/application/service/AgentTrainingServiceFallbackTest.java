package id.payu.support.application.service;

import id.payu.support.domain.port.out.AgentTrainingRepositoryPort;
import id.payu.support.domain.port.out.SupportAgentRepositoryPort;
import id.payu.support.domain.port.out.TrainingModuleRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AgentTrainingServiceFallbackTest {

    @Mock
    private AgentTrainingRepositoryPort agentTrainingRepository;
    @Mock
    private SupportAgentRepositoryPort agentRepository;
    @Mock
    private TrainingModuleRepositoryPort moduleRepository;

    private AgentTrainingService service() {
        return new AgentTrainingService(agentTrainingRepository, agentRepository, moduleRepository);
    }

    private Object invokeFallback(String method, Object... args) throws Exception {
        Method m = AgentTrainingService.class.getDeclaredMethod(method, toParamTypes(method, args));
        m.setAccessible(true);
        try {
            return m.invoke(service(), args);
        } catch (InvocationTargetException e) {
            // unwrap the real exception thrown by the fallback body
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        }
    }

    private Class<?>[] toParamTypes(String method, Object... args) throws Exception {
        for (Method m : AgentTrainingService.class.getDeclaredMethods()) {
            if (m.getName().equals(method) && m.getParameterCount() == args.length) {
                return m.getParameterTypes();
            }
        }
        throw new NoSuchMethodException(method);
    }

    @Test
    void fallbackRethrowsBusinessException() throws Exception {
        var ex = new IllegalArgumentException("Agent not found");
        assertThatThrownBy(() -> invokeFallback("getTrainingsByAgentFallback", 1L, ex))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent not found");
    }

    @Test
    void fallbackRethrowsDataIntegrityViolation() throws Exception {
        var ex = new DataIntegrityViolationException("integrity");
        assertThatThrownBy(() -> invokeFallback("getTrainingsByModuleFallback", 1L, ex))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void fallbackRethrowsHttpMessageNotReadable() throws Exception {
        var ex = new HttpMessageNotReadableException("unreadable",
                new RuntimeException("root"),
                org.mockito.Mockito.mock(org.springframework.http.HttpInputMessage.class));
        assertThatThrownBy(() -> invokeFallback("assignTrainingFallback",
                new id.payu.support.interfaces.dto.AssignTrainingRequest(1L, 1L, null, null, null), ex))
                .isInstanceOf(HttpMessageNotReadableException.class);
    }

    @Test
    void fallbackWrapsGenericException() {
        var ex = new RuntimeException("rules down");
        assertThatThrownBy(() -> invokeFallback("getAllAgentTrainingsFallback", ex))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Support service temporarily unavailable")
                .hasCause(ex);
    }
}
