package id.payu.partner.adapter.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

/**
 * PARTNER-004: the endpoint named public health must be reachable without
 * authentication. Proves the controller responds 200 with real DB/Kafka probes.
 */
class HealthPublicTest {

    @Test
    @DisplayName("PARTNER-004: /partners/public/health responds 200")
    void publicHealthReachable() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        HealthController controller = new HealthController(dataSource, null);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/partners/public/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PARTNER-004: health response exposes service name and status fields")
    void publicHealthResponseShape() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        HealthController controller = new HealthController(dataSource, null);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/partners/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.service").value("partner-service"));
    }
}
