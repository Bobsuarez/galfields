package co.com.galfields.pos_transactions.api.auth;

import java.util.Map;

public record LoginResponse(
        String token,
        Long employeeId,
        String username,
        Long roleId,
        String roleName,
        Map<String, Boolean> permissions,
        Long terminalId) {
}
