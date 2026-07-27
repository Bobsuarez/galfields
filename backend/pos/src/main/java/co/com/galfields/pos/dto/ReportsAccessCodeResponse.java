package co.com.galfields.pos.dto;

import java.time.LocalDateTime;

public record ReportsAccessCodeResponse(String code, LocalDateTime generatedAt) {
}
