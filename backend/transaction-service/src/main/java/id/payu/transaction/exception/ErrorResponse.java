package id.payu.transaction.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private boolean success;
    private ErrorDetail error;
    private Meta meta;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ErrorDetail {
    private String code;
    private String message;
    private Map<String, Object> details;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Meta {
    private String requestId;
    private Instant timestamp;
}
