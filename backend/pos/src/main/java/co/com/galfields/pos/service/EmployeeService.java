package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.EmployeeRequest;
import co.com.galfields.pos.dto.EmployeeResponse;
import co.com.galfields.pos.entity.AttachFile;
import co.com.galfields.pos.entity.Employee;
import co.com.galfields.pos.entity.EmployeeRole;
import co.com.galfields.pos.entity.Terminal;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.AttachFileRepository;
import co.com.galfields.pos.repository.EmployeeRepository;
import co.com.galfields.pos.repository.EmployeeRoleRepository;
import co.com.galfields.pos.repository.TerminalRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    // Shared placeholder for employees.logo_image (BIGSERIAL -> NOT NULL in
    // production, see this repo's CLAUDE.md) - this CRUD has no photo
    // upload, seeded by V9__employee_auth.sql.
    private static final String DEFAULT_LOGO_NAME = "no-employee-photo";

    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final TerminalRepository terminalRepository;
    private final AttachFileRepository attachFileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("password is required to create an employee");
        }
        Employee employee = new Employee();
        employee.setLogoImage(defaultLogoImage());
        applyFields(employee, request);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long employeeId) {
        return toResponse(findOrThrow(employeeId));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long employeeId, EmployeeRequest request) {
        Employee employee = findOrThrow(employeeId);
        applyFields(employee, request);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void deactivateEmployee(Long employeeId) {
        Employee employee = findOrThrow(employeeId);
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private Employee findOrThrow(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + employeeId + " not found"));
    }

    private AttachFile defaultLogoImage() {
        return attachFileRepository.findByName(DEFAULT_LOGO_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Default employee logo placeholder '" + DEFAULT_LOGO_NAME + "' is missing - check V9__employee_auth.sql ran"));
    }

    private void applyFields(Employee employee, EmployeeRequest request) {
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            employee.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        EmployeeRole role = employeeRoleRepository.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee role " + request.roleId() + " not found"));
        employee.setRole(role);

        List<Long> terminalIds = request.terminalIds() == null ? List.of() : request.terminalIds();
        Set<Terminal> terminals = new HashSet<>(terminalRepository.findAllById(terminalIds));
        if (terminals.size() != terminalIds.size()) {
            throw new ResourceNotFoundException("One or more terminalIds do not exist: " + terminalIds);
        }
        employee.setTerminals(terminals);
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getEmployeeId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getUsername(),
                employee.getRole().getRoleId(),
                employee.getRole().getRoleName(),
                employee.getTerminals().stream().map(Terminal::getTerminalId).toList(),
                employee.isActive(),
                employee.getCreatedAt());
    }
}
