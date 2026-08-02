package co.com.galfields.pos_transactions.jpa.employee;

import co.com.galfields.pos_transactions.model.employee.EmployeeRole;
import co.com.galfields.pos_transactions.model.employee.gateways.EmployeeRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmployeeRoleRepositoryAdapter implements EmployeeRoleRepository {

    private final EmployeeRoleJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<EmployeeRole> findById(Long roleId) {
        return repository.findById(roleId).map(this::toDomain);
    }

    @Override
    public List<EmployeeRole> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public EmployeeRole save(EmployeeRole role) {
        EmployeeRoleEntity entity = new EmployeeRoleEntity();
        entity.setRoleId(role.getRoleId());
        entity.setRoleName(role.getRoleName());
        entity.setPermissions(writePermissions(role.getPermissions()));
        entity.setCanLoginMobile(role.isCanLoginMobile());
        entity.setCanLoginDesktop(role.isCanLoginDesktop());
        entity.setCreatedAt(role.getCreatedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long roleId) {
        repository.deleteById(roleId);
    }

    private EmployeeRole toDomain(EmployeeRoleEntity entity) {
        return EmployeeRole.builder()
                .roleId(entity.getRoleId())
                .roleName(entity.getRoleName())
                .permissions(readPermissions(entity.getPermissions()))
                .canLoginMobile(entity.isCanLoginMobile())
                .canLoginDesktop(entity.isCanLoginDesktop())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Map<String, Boolean> readPermissions(String json) {
        if (json == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Boolean>>() {
            });
        } catch (JacksonException e) {
            throw new IllegalStateException("Corrupt permissions JSON for employee_roles row", e);
        }
    }

    private String writePermissions(Map<String, Boolean> permissions) {
        try {
            return objectMapper.writeValueAsString(permissions);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Invalid permissions payload", e);
        }
    }
}
