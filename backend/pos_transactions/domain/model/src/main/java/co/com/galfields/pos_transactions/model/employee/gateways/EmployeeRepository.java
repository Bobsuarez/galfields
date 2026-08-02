package co.com.galfields.pos_transactions.model.employee.gateways;

import co.com.galfields.pos_transactions.model.employee.Employee;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Optional<Employee> findById(Long employeeId);

    Optional<Employee> findByUsername(String username);

    List<Employee> findAll();

    Employee save(Employee employee);
}
