package id.payu.partner.adapter.web.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * Request wrapper that adds sandbox-related headers to the request.
 * Used by SandboxFilter to propagate sandbox mode to downstream services.
 */
public class SandboxHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private static final String SANDBOX_MODE_HEADER = "X-Sandbox-Mode";
    private final boolean sandboxMode;
    private final Map<String, String> customHeaders;

    public SandboxHttpServletRequestWrapper(HttpServletRequest request, boolean sandboxMode) {
        super(request);
        this.sandboxMode = sandboxMode;
        this.customHeaders = new HashMap<>();

        if (sandboxMode) {
            customHeaders.put(SANDBOX_MODE_HEADER, "true");
        }
    }

    @Override
    public String getHeader(String name) {
        if (customHeaders.containsKey(name)) {
            return customHeaders.get(name);
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(Collections.singletonList(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> headerNames = new HashSet<>();
        Enumeration<String> originalNames = super.getHeaderNames();
        while (originalNames.hasMoreElements()) {
            headerNames.add(originalNames.nextElement());
        }
        headerNames.addAll(customHeaders.keySet());
        return Collections.enumeration(headerNames);
    }

    public boolean isSandboxMode() {
        return sandboxMode;
    }
}
