package co.com.galfields.pos_transactions.jpa.sale.shadow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only shadow of {@code employees}, scoped to what Sales needs (resolve
 * the placeholder "pos-terminal" employee id). The real Empleados module
 * (Fase 5) will bring its own full entity over the same table — see spec
 * 04's Fase 2/3 coordination note and this repo's AskUserQuestion decision.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
public class EmployeeShadowEntity {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;
}
