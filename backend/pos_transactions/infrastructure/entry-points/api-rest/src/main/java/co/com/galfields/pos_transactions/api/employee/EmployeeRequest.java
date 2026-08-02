package co.com.galfields.pos_transactions.api.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EmployeeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String username,
        /** Required on create; blank/null on update leaves the current password_hash untouched. */
        String password,
        @NotNull Long roleId,
        List<Long> terminalIds) {
}
