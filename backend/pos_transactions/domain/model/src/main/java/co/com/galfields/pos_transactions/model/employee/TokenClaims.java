package co.com.galfields.pos_transactions.model.employee;

import java.util.Map;

public record TokenClaims(
        Long employeeId,
        String username,
        Long roleId,
        String roleName,
        Map<String, Boolean> permissions,
        boolean canLoginMobile,
        boolean canLoginDesktop,
        /** Null on mobile logins. */
        Long terminalId) {
}
