package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
