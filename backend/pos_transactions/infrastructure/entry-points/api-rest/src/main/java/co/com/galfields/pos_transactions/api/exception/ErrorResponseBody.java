package co.com.galfields.pos_transactions.api.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mirrors backend/pos's ErrorResponseBody — same shape across every clean
 * error response this API returns. */
public final class ErrorResponseBody {

    private ErrorResponseBody() {
    }

    public static Map<String, Object> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
