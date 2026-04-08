package id.payu.account.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class FeignRequestHeaderPropagationConfigTest {

    private final FeignRequestHeaderPropagationConfig config = new FeignRequestHeaderPropagationConfig();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldPropagateE2ETestHeaderWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(FeignRequestHeaderPropagationConfig.E2E_TEST_HEADER, "crud-stress");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestInterceptor interceptor = config.requestHeaderPropagationInterceptor();
        RequestTemplate requestTemplate = new RequestTemplate();

        interceptor.apply(requestTemplate);

        assertThat(requestTemplate.headers())
            .containsKey(FeignRequestHeaderPropagationConfig.E2E_TEST_HEADER);
        assertThat(requestTemplate.headers().get(FeignRequestHeaderPropagationConfig.E2E_TEST_HEADER))
            .containsExactly("crud-stress");
    }

    @Test
    void shouldSkipE2ETestHeaderWhenAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestInterceptor interceptor = config.requestHeaderPropagationInterceptor();
        RequestTemplate requestTemplate = new RequestTemplate();

        interceptor.apply(requestTemplate);

        assertThat(requestTemplate.headers())
            .doesNotContainKey(FeignRequestHeaderPropagationConfig.E2E_TEST_HEADER);
    }
}