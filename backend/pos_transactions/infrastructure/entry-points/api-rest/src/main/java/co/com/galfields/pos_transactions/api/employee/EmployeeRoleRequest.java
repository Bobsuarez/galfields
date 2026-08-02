package co.com.galfields.pos_transactions.api.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record EmployeeRoleRequest(
        @NotBlank String roleName,
        @NotNull Map<String, Boolean> permissions,
        @NotNull Boolean canLoginMobile,
        @NotNull Boolean canLoginDesktop) {
}
