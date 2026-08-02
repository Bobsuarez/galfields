package co.com.galfields.pos_transactions.api.reportsaccess;

import jakarta.validation.constraints.NotBlank;

public record ReportsAccessCodeValidateRequest(@NotBlank String code) {
}
