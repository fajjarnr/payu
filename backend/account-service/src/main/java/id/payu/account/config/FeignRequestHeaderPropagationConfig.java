package id.payu.account.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration(proxyBeanMethods = false)
public class FeignRequestHeaderPropagationConfig {

    static final String E2E_TEST_HEADER = "X-E2E-Test";

    @Bean
    RequestInterceptor requestHeaderPropagationInterceptor() {
        return requestTemplate -> {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
                return;
            }

            HttpServletRequest request = servletRequestAttributes.getRequest();
            String e2eTestHeader = request.getHeader(E2E_TEST_HEADER);
            if (StringUtils.hasText(e2eTestHeader)) {
                requestTemplate.header(E2E_TEST_HEADER, e2eTestHeader);
            }
        };
    }
}