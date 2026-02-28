package id.payu.productcatalog.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base controller providing common response utilities.
 */
public abstract class BaseController {

    protected <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    protected <T> ResponseEntity<T> created(String path, T body) {
        return ResponseEntity.created(URI.create(path)).body(body);
    }

    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    protected <T> ResponseEntity<List<T>> okList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(list);
    }

    protected ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }
}
