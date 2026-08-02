package co.com.galfields.pos_transactions.model.employee.gateways;

import co.com.galfields.pos_transactions.model.employee.EmployeeRole;

import java.util.List;
import java.util.Optional;

public interface EmployeeRoleRepository {
    Optional<EmployeeRole> findById(Long roleId);

    List<EmployeeRole> findAll();

    EmployeeRole save(EmployeeRole role);

    void deleteById(Long roleId);
}
