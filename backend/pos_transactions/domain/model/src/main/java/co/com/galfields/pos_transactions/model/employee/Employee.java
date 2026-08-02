package co.com.galfields.pos_transactions.model.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Employee {
    private Long employeeId;
    private String firstName;
    private String lastName;
    private String username;
    /** Bcrypt hash — never echoed back in a response. */
    private String passwordHash;
    private Long roleId;
    private String roleName;
    private List<Long> terminalIds;
    private boolean active;
    private LocalDateTime createdAt;
}
