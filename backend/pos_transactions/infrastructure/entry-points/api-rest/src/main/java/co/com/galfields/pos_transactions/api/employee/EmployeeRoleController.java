package co.com.galfields.pos_transactions.api.employee;

import co.com.galfields.pos_transactions.model.employee.EmployeeRole;
import co.com.galfields.pos_transactions.usecase.employee.EmployeeRoleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Mirrors backend/pos's EmployeeRoleController 1:1. */
@RestController
@RequestMapping("/api/employee-roles")
@RequiredArgsConstructor
public class EmployeeRoleController {

    private final EmployeeRoleUseCase employeeRoleUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EmployeeRoleResponse create(@RequestBody @Valid EmployeeRoleRequest request) {
        return toResponse(employeeRoleUseCase.create(toDomain(request)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EmployeeRoleResponse> list() {
        return employeeRoleUseCase.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{roleId}")
    @Transactional(readOnly = true)
    public EmployeeRoleResponse get(@PathVariable("roleId") Long roleId) {
        return toResponse(employeeRoleUseCase.get(roleId));
    }

    @PutMapping("/{roleId}")
    @Transactional
    public EmployeeRoleResponse update(@PathVariable("roleId") Long roleId, @RequestBody @Valid EmployeeRoleRequest request) {
        return toResponse(employeeRoleUseCase.update(roleId, toDomain(request)));
    }

    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("roleId") Long roleId) {
        employeeRoleUseCase.delete(roleId);
    }

    private EmployeeRole toDomain(EmployeeRoleRequest request) {
        return EmployeeRole.builder()
                .roleName(request.roleName())
                .permissions(request.permissions())
                .canLoginMobile(request.canLoginMobile())
                .canLoginDesktop(request.canLoginDesktop())
                .build();
    }

    private EmployeeRoleResponse toResponse(EmployeeRole role) {
        return new EmployeeRoleResponse(role.getRoleId(), role.getRoleName(), role.getPermissions(),
                role.isCanLoginMobile(), role.isCanLoginDesktop(), role.getCreatedAt());
    }
}
