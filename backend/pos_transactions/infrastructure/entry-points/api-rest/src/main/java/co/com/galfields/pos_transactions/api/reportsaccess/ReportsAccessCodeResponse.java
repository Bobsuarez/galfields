package co.com.galfields.pos_transactions.api.reportsaccess;

import java.time.LocalDateTime;

public record ReportsAccessCodeResponse(String code, LocalDateTime generatedAt) {
}
