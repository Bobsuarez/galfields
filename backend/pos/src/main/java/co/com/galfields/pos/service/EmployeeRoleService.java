package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.EmployeeRoleRequest;
import co.com.galfields.pos.dto.EmployeeRoleResponse;
import co.com.galfields.pos.entity.EmployeeRole;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.EmployeeRoleRepository;
import co.com.galfields.pos.util.PermissionsJson;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class EmployeeRoleService {

    private final EmployeeRoleRepository employeeRoleRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public EmployeeRoleResponse createRole(EmployeeRoleRequest request) {
        EmployeeRole role = new EmployeeRole();
        applyFields(role, request);
        return toResponse(employeeRoleRepository.save(role));
    }

    @Transactional(readOnly = true)
    public EmployeeRoleResponse getRole(Long roleId) {
        return toResponse(findOrThrow(roleId));
    }

    @Transactional(readOnly = true)
    public List<EmployeeRoleResponse> listRoles() {
        return employeeRoleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeRoleResponse updateRole(Long roleId, EmployeeRoleRequest request) {
        EmployeeRole role = findOrThrow(roleId);
        applyFields(role, request);
        return toResponse(employeeRoleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long roleId) {
        EmployeeRole role = findOrThrow(roleId);
        employeeRoleRepository.delete(role);
    }

    private EmployeeRole findOrThrow(Long roleId) {
        return employeeRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee role " + roleId + " not found"));
    }

    private void applyFields(EmployeeRole role, EmployeeRoleRequest request) {
        role.setRoleName(request.roleName());
        role.setPermissions(writePermissions(request.permissions()));
        role.setCanLoginMobile(request.canLoginMobile());
        role.setCanLoginDesktop(request.canLoginDesktop());
    }

    private EmployeeRoleResponse toResponse(EmployeeRole role) {
        return new EmployeeRoleResponse(
                role.getRoleId(),
                role.getRoleName(),
                PermissionsJson.read(objectMapper, role.getPermissions()),
                role.isCanLoginMobile(),
                role.isCanLoginDesktop(),
                role.getCreatedAt());
    }

    private String writePermissions(Map<String, Boolean> permissions) {
        try {
            return objectMapper.writeValueAsString(permissions);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Invalid permissions payload", e);
        }
    }
}
