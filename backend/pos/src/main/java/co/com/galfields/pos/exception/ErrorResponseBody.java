package co.com.galfields.pos.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

// Shared by GlobalExceptionHandler (controller-level exceptions) and
// SecurityConfig's AuthenticationEntryPoint/AccessDeniedHandler (rejections
// that never reach a controller) - both need byte-identical error bodies,
// per this spec's requirement that a 401/403 from the security layer look
// exactly like every other clean error response.
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
