package co.com.galfields.pos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeResponse(
        Long employeeId,
        String firstName,
        String lastName,
        String username,
        Long roleId,
        String roleName,
        List<Long> terminalIds,
        boolean active,
        LocalDateTime createdAt) {
}
