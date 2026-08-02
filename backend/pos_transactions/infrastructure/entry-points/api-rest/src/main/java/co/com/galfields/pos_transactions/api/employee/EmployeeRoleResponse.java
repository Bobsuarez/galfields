package co.com.galfields.pos_transactions.api.employee;

import java.time.LocalDateTime;
import java.util.Map;

public record EmployeeRoleResponse(
        Long roleId,
        String roleName,
        Map<String, Boolean> permissions,
        boolean canLoginMobile,
        boolean canLoginDesktop,
        LocalDateTime createdAt) {
}
