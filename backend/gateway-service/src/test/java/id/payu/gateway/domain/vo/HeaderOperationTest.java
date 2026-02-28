package id.payu.gateway.domain.vo;

import id.payu.gateway.domain.entity.TransformationRule;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HeaderOperation value object.
 */
class HeaderOperationTest {

    @Test
    void shouldCreateAddOperation() {
        HeaderOperation op = HeaderOperation.add("X-Custom-Header", "value");

        assertEquals(HeaderOperation.Type.ADD, op.type());
        assertEquals("X-Custom-Header", op.headerName());
        assertEquals("value", op.value());
    }

    @Test
    void shouldCreateAddIfMissingOperation() {
        HeaderOperation op = HeaderOperation.addIfMissing("X-Custom-Header", "value");

        assertEquals(HeaderOperation.Type.ADD_IF_MISSING, op.type());
        assertEquals("X-Custom-Header", op.headerName());
        assertEquals("value", op.value());
    }

    @Test
    void shouldCreateRemoveOperation() {
        HeaderOperation op = HeaderOperation.remove("X-Remove-Header");

        assertEquals(HeaderOperation.Type.REMOVE, op.type());
        assertEquals("X-Remove-Header", op.headerName());
        assertNull(op.value());
    }

    @Test
    void shouldCreateRewriteOperation() {
        HeaderOperation op = HeaderOperation.rewrite("X-Rewrite-Header", "new-value");

        assertEquals(HeaderOperation.Type.REWRITE, op.type());
        assertEquals("X-Rewrite-Header", op.headerName());
        assertEquals("new-value", op.value());
    }

    @Test
    void shouldThrowExceptionForNullType() {
        assertThrows(NullPointerException.class, () ->
            new HeaderOperation(null, "header", "value", null));
    }

    @Test
    void shouldThrowExceptionForNullHeaderName() {
        assertThrows(NullPointerException.class, () ->
            new HeaderOperation(HeaderOperation.Type.ADD, null, "value", null));
    }

    @Test
    void shouldApplyAddOperation() {
        HeaderOperation op = HeaderOperation.add("X-Custom", "value1");

        Map<String, List<String>> headers = new HashMap<>();
        TransformationRule.TransformationContext ctx =
            new TransformationRule.TransformationContext("/api", "GET", headers, null);

        op.apply(ctx);

        assertTrue(ctx.getHeaders().containsKey("X-Custom"));
        assertEquals(List.of("value1"), ctx.getHeaders().get("X-Custom"));
    }

    @Test
    void shouldApplyAddOperationWithExistingHeader() {
        HeaderOperation op = HeaderOperation.add("X-Custom", "value2");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Custom", new ArrayList<>(List.of("value1")));

        TransformationRule.TransformationContext ctx =
            new TransformationRule.TransformationContext("/api", "GET", headers, null);

        op.apply(ctx);

        assertEquals(List.of("value1", "value2"), ctx.getHeaders().get("X-Custom"));
    }

    @Test
    void shouldApplyAddIfMissingOperation() {
        HeaderOperation op = HeaderOperation.addIfMissing("X-Custom", "value");

        Map<String, List<String>> headers = new HashMap<>();
        TransformationRule.TransformationContext ctx =
            new TransformationRule.TransformationContext("/api", "GET", headers, null);

        op.apply(ctx);

        assertTrue(ctx.getHeaders().containsKey("X-Custom"));
        assertEquals(List.of("value"), ctx.getHeaders().get("X-Custom"));
    }

    @Test
    void shouldNotAddIfHeaderExists() {
        HeaderOperation op = HeaderOperation.addIfMissing("X-Custom", "new-value");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Custom", new ArrayList<>(List.of("existing-value")));

        TransformationRule.TransformationContext ctx =
            new TransformationRule.TransformationContext("/api", "GET", headers, null);

        op.apply(ctx);

        assertEquals(List.of("existing-value"), ctx.getHeaders().get("X-Custom"));
    }

    @Test
    void shouldApplyRemoveOperation() {
        HeaderOperation op = HeaderOperation.remove("X-Remove");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Remove", new ArrayList<>(List.of("value")));
        headers.put("X-Keep", new ArrayList<>(List.of("value")));

        TransformationRule.TransformationContext ctx =
            new TransformationRule.TransformationContext("/api", "GET", headers, null);

        op.apply(ctx);

        assertFalse(ctx.getHeaders().containsKey("X-Remove"));
        assertTrue(ctx.getHeaders().containsKey("X-Keep"));
    }

    @Test
    void shouldApplyRewriteOperation() {
        HeaderOperation op = HeaderOperation.rewrite("X-Rewrite", "new-value");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Rewrite", new ArrayList<>(List.of("old-value1", "old-value2")));

        TransformationRule.TransformationContext ctx =
            new TransformationRule.TransformationContext("/api", "GET", headers, null);

        op.apply(ctx);

        assertEquals(List.of("new-value"), ctx.getHeaders().get("X-Rewrite"));
    }
}
