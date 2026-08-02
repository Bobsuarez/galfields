package co.com.galfields.pos_transactions.jpa.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_terminals")
@IdClass(EmployeeTerminalId.class)
@Getter
@Setter
@NoArgsConstructor
public class EmployeeTerminalEntity {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @Id
    @Column(name = "terminal_id")
    private Long terminalId;

    public EmployeeTerminalEntity(Long employeeId, Long terminalId) {
        this.employeeId = employeeId;
        this.terminalId = terminalId;
    }
}
