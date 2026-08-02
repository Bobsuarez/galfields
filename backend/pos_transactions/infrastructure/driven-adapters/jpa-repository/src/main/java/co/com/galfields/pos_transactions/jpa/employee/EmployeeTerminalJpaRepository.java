package co.com.galfields.pos_transactions.jpa.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeTerminalJpaRepository extends JpaRepository<EmployeeTerminalEntity, EmployeeTerminalId> {
    List<EmployeeTerminalEntity> findByEmployeeId(Long employeeId);
}
