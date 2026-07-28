package co.com.galfields.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record EmployeeRoleRequest(
        @NotBlank String roleName,
        // Boolean map per module, e.g. { "pos": true, "inventario": false,
        // "reportes": false, "sync": true } - not restricted to a fixed set
        // of keys, so a new module can gate on a permission here without a
        // code change.
        @NotNull Map<String, Boolean> permissions,
        @NotNull Boolean canLoginMobile,
        @NotNull Boolean canLoginDesktop) {
}
