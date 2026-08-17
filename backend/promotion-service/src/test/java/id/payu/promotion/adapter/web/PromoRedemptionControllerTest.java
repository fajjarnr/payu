package id.payu.promotion.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import id.payu.promotion.application.service.PromoRedemptionService;
import id.payu.promotion.interfaces.dto.ApplyPromoRequest;
import id.payu.promotion.interfaces.dto.ApplyPromoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromoRedemptionControllerTest {

    @Test
    void validationDelegatesToReadOnlyServicePath() {
        PromoRedemptionService service = mock(PromoRedemptionService.class);
        PromoRedemptionController controller = new PromoRedemptionController(service);
        ApplyPromoRequest request = new ApplyPromoRequest(
                "PREVIEW10", "user-1", "validation", new BigDecimal("100"), null
        );
        when(service.validatePromo(request)).thenReturn(
                ApplyPromoResponse.success("PREVIEW10", new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90")));

        assertEquals(200, controller.validatePromo("PREVIEW10", new BigDecimal("100"), "user-1").getStatusCode().value());
        verify(service).validatePromo(any(ApplyPromoRequest.class));
        verify(service, never()).applyPromo(any(ApplyPromoRequest.class));
    }

    @Test
    void applyRequiresDistributedIdempotencyHeader() throws NoSuchMethodException {
        Method method = PromoRedemptionController.class.getDeclaredMethod(
                "applyPromo", ApplyPromoRequest.class, String.class);
        RequestHeader header = (RequestHeader) method.getParameterAnnotations()[1][0];
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        assertTrue(header.required());
        assertNotNull(idempotent);
        assertTrue(idempotent.required());
        assertEquals("X-Idempotency-Key", idempotent.headerName());
    }
}
