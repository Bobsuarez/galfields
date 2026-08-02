package co.com.galfields.pos_transactions.jpa.employee;

import co.com.galfields.pos_transactions.model.employee.Employee;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    // Shared placeholder for employees.logo_image (BIGSERIAL -> NOT NULL in
    // production, see backend/pos/CLAUDE.md) — this CRUD has no photo
    // upload, seeded by V9__employee_auth.sql.
    private static final String DEFAULT_LOGO_NAME = "no-employee-photo";

    private final EmployeeJpaRepository repository;
    private final EmployeeRoleJpaRepository roleRepository;
    private final EmployeeTerminalJpaRepository employeeTerminalRepository;
    private final AttachFileRefJpaRepository attachFileRepository;

    @Override
    public Optional<Employee> findById(Long employeeId) {
        return repository.findById(employeeId).map(this::toDomain);
    }

    @Override
    public Optional<Employee> findByUsername(String username) {
        return repository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public List<Employee> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Employee save(Employee employee) {
        boolean isNew = employee.getEmployeeId() == null;

        EmployeeEntity entity = new EmployeeEntity();
        entity.setEmployeeId(employee.getEmployeeId());
        entity.setFirstName(employee.getFirstName());
        entity.setLastName(employee.getLastName());
        entity.setUsername(employee.getUsername());
        entity.setPasswordHash(employee.getPasswordHash());
        entity.setRoleId(employee.getRoleId());
        entity.setActive(employee.isActive());
        entity.setCreatedAt(employee.getCreatedAt());
        entity.setLogoImage(isNew ? defaultLogoImageId() : employee.getEmployeeId() != null ? existingLogoImage(employee.getEmployeeId()) : null);

        EmployeeEntity saved = repository.save(entity);
        syncTerminals(saved.getEmployeeId(), employee.getTerminalIds());

        return toDomain(saved);
    }

    private Long existingLogoImage(Long employeeId) {
        return repository.findById(employeeId).map(EmployeeEntity::getLogoImage).orElse(defaultLogoImageId());
    }

    private Long defaultLogoImageId() {
        return attachFileRepository.findByName(DEFAULT_LOGO_NAME)
                .map(AttachFileRefEntity::getAttachFilesId)
                .orElseThrow(() -> new IllegalStateException(
                        "Default employee logo placeholder '" + DEFAULT_LOGO_NAME + "' is missing - check V9__employee_auth.sql ran"));
    }

    private void syncTerminals(Long employeeId, List<Long> terminalIds) {
        List<Long> desired = terminalIds == null ? List.of() : terminalIds;
        List<EmployeeTerminalEntity> existing = employeeTerminalRepository.findByEmployeeId(employeeId);
        if (!existing.isEmpty()) {
            employeeTerminalRepository.deleteAll(existing);
            employeeTerminalRepository.flush();
        }
        for (Long terminalId : desired) {
            employeeTerminalRepository.save(new EmployeeTerminalEntity(employeeId, terminalId));
        }
    }

    private Employee toDomain(EmployeeEntity entity) {
        String roleName = entity.getRoleId() == null ? null
                : roleRepository.findById(entity.getRoleId()).map(EmployeeRoleEntity::getRoleName).orElse(null);
        List<Long> terminalIds = employeeTerminalRepository.findByEmployeeId(entity.getEmployeeId()).stream()
                .map(EmployeeTerminalEntity::getTerminalId)
                .toList();

        return Employee.builder()
                .employeeId(entity.getEmployeeId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .username(entity.getUsername())
                .passwordHash(entity.getPasswordHash())
                .roleId(entity.getRoleId())
                .roleName(roleName)
                .terminalIds(terminalIds)
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
