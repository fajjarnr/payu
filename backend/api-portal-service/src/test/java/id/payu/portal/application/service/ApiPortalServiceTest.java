package id.payu.portal.application.service;

import id.payu.portal.dto.ServiceListResponse;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ApiPortalServiceTest {

    @Inject
    ApiPortalService apiPortalService;

    @Test
    void testListServices() {
        ServiceListResponse response = apiPortalService.listServices().await().indefinitely();
        assertNotNull(response);
        assertNotNull(response.services());
        assertFalse(response.services().isEmpty());
        assertEquals(20, response.services().size());
    }
}
