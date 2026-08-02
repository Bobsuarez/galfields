package co.com.galfields.pos_transactions.model.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class EmployeeRole {
    private Long roleId;
    private String roleName;
    /** Per-module boolean map, e.g. {"pos": true, "inventario": false} — not
     * restricted to a fixed key set, so a new module can gate on a
     * permission here without a code change. */
    private Map<String, Boolean> permissions;
    private boolean canLoginMobile;
    private boolean canLoginDesktop;
    private LocalDateTime createdAt;
}
