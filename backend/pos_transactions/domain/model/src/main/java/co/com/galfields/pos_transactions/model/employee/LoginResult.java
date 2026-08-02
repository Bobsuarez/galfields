package co.com.galfields.pos_transactions.model.employee;

import java.util.Map;

public record LoginResult(
        String token,
        Long employeeId,
        String username,
        Long roleId,
        String roleName,
        Map<String, Boolean> permissions,
        /** Null on mobile logins — only set when the login carried a terminalCode. */
        Long terminalId) {
}
