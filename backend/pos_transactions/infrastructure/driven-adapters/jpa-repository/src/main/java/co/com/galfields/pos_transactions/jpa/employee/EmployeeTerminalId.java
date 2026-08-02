package co.com.galfields.pos_transactions.jpa.employee;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeTerminalId implements Serializable {
    private Long employeeId;
    private Long terminalId;
}
