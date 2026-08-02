package co.com.galfields.pos_transactions.usecase.employee;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.employee.EmployeeRole;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRoleRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Mirrors backend/pos's EmployeeRoleService — plain CRUD, hard delete
 * (unlike Employee's soft-deactivate). */
@RequiredArgsConstructor
public class EmployeeRoleUseCase {

    private final EmployeeRoleRepository employeeRoleRepository;

    public EmployeeRole create(EmployeeRole role) {
        return employeeRoleRepository.save(role);
    }

    public EmployeeRole get(Long roleId) {
        return findOrThrow(roleId);
    }

    public List<EmployeeRole> list() {
        return employeeRoleRepository.findAll();
    }

    public EmployeeRole update(Long roleId, EmployeeRole patch) {
        EmployeeRole existing = findOrThrow(roleId);
        existing.setRoleName(patch.getRoleName());
        existing.setPermissions(patch.getPermissions());
        existing.setCanLoginMobile(patch.isCanLoginMobile());
        existing.setCanLoginDesktop(patch.isCanLoginDesktop());
        return employeeRoleRepository.save(existing);
    }

    public void delete(Long roleId) {
        findOrThrow(roleId);
        employeeRoleRepository.deleteById(roleId);
    }

    private EmployeeRole findOrThrow(Long roleId) {
        return employeeRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee role " + roleId + " not found"));
    }
}
