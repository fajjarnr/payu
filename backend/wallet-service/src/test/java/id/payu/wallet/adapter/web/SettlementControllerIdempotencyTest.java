package id.payu.wallet.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.commons.idempotency.Idempotent;
import id.payu.commons.idempotency.IdempotencyInterceptor;
import id.payu.commons.idempotency.IdempotencyService;
import id.payu.wallet.domain.port.in.SettlementUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettlementControllerIdempotencyTest {

    @Test
    void shouldRequireIdempotencyKeyForEverySettlementMutation() {
        Arrays.stream(SettlementController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .forEach(this::assertIdempotencyRequired);
    }

    @Test
    void shouldRejectMissingIdempotencyKeyBeforeCallingUseCase() throws Exception {
        SettlementUseCase useCase = mock(SettlementUseCase.class);
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(
                mock(IdempotencyService.class), new ObjectMapper());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new SettlementController(useCase))
                .addInterceptors(interceptor)
                .build();

        mockMvc.perform(post("/api/v1/settlements/batches/{batchId}/process", UUID.randomUUID())
                        .param("processedBy", "backoffice-user"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required header: X-Idempotency-Key"));

        verifyNoInteractions(useCase);
    }

    private void assertIdempotencyRequired(Method method) {
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        assertTrue(idempotent != null && idempotent.required(),
                () -> method.getName() + " must require X-Idempotency-Key");
        assertEquals("X-Idempotency-Key", idempotent.headerName(),
                () -> method.getName() + " must use platform idempotency header");
    }
}
