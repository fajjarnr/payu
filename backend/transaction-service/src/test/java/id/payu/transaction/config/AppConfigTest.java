package id.payu.transaction.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    @Test
    void restTemplateHasBoundedTimeouts() throws Exception {
        var requestFactory = (SimpleClientHttpRequestFactory) new AppConfig().restTemplate().getRequestFactory();

        assertThat(timeoutField(requestFactory, "connectTimeout")).isEqualTo(5_000);
        assertThat(timeoutField(requestFactory, "readTimeout")).isEqualTo(10_000);
    }

    private static int timeoutField(SimpleClientHttpRequestFactory factory, String name) throws Exception {
        Field field = SimpleClientHttpRequestFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(factory);
    }
}
