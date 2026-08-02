package co.com.galfields.pos_transactions.usecase.employee;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.employee.Employee;
import co.com.galfields.pos_transactions.model.employee.EmployeeRole;
import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRoleRepository;
import co.com.galfields.pos_transactions.model.employee.gateways.PasswordHasher;
import co.com.galfields.pos_transactions.model.employee.gateways.TerminalRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Mirrors backend/pos's EmployeeService. {@code password} is required on
 * create, optional on update (blank/null leaves password_hash untouched —
 * the deliberate "admin resets a forgotten password" mechanism, no separate
 * reset flow). {@code logoImage} always defaults to the shared placeholder
 * row — no photo upload in this CRUD.
 */
@RequiredArgsConstructor
public class EmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final TerminalRepository terminalRepository;
    private final PasswordHasher passwordHasher;

    public Employee create(Employee employee, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password is required to create an employee");
        }
        employee.setPasswordHash(passwordHasher.hash(rawPassword));
        resolveRoleAndTerminals(employee);
        return employeeRepository.save(employee);
    }

    public Employee get(Long employeeId) {
        return findOrThrow(employeeId);
    }

    public List<Employee> list() {
        return employeeRepository.findAll();
    }

    public Employee update(Long employeeId, Employee patch, String rawPassword) {
        Employee existing = findOrThrow(employeeId);
        existing.setFirstName(patch.getFirstName());
        existing.setLastName(patch.getLastName());
        existing.setUsername(patch.getUsername());
        existing.setRoleId(patch.getRoleId());
        existing.setTerminalIds(patch.getTerminalIds());
        if (rawPassword != null && !rawPassword.isBlank()) {
            existing.setPasswordHash(passwordHasher.hash(rawPassword));
        }
        resolveRoleAndTerminals(existing);
        return employeeRepository.save(existing);
    }

    /** Soft-deactivate, not a hard delete. */
    public void deactivate(Long employeeId) {
        Employee employee = findOrThrow(employeeId);
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private void resolveRoleAndTerminals(Employee employee) {
        EmployeeRole role = employeeRoleRepository.findById(employee.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee role " + employee.getRoleId() + " not found"));
        employee.setRoleName(role.getRoleName());

        List<Long> terminalIds = employee.getTerminalIds() == null ? List.of() : employee.getTerminalIds();
        List<Terminal> found = terminalRepository.findAllExisting(terminalIds);
        if (found.size() != terminalIds.size()) {
            throw new ResourceNotFoundException("One or more terminalIds do not exist: " + terminalIds);
        }
        employee.setTerminalIds(terminalIds);
    }

    private Employee findOrThrow(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + employeeId + " not found"));
    }
}
