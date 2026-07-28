package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.EmployeeRoleRequest;
import co.com.galfields.pos.dto.EmployeeRoleResponse;
import co.com.galfields.pos.service.EmployeeRoleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee-roles")
@RequiredArgsConstructor
public class EmployeeRoleController {

    private final EmployeeRoleService employeeRoleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeRoleResponse create(@RequestBody @Valid EmployeeRoleRequest request) {
        return employeeRoleService.createRole(request);
    }

    @GetMapping
    public List<EmployeeRoleResponse> list() {
        return employeeRoleService.listRoles();
    }

    @GetMapping("/{roleId}")
    public EmployeeRoleResponse get(@PathVariable Long roleId) {
        return employeeRoleService.getRole(roleId);
    }

    @PutMapping("/{roleId}")
    public EmployeeRoleResponse update(
            @PathVariable Long roleId,
            @RequestBody @Valid EmployeeRoleRequest request
    ) {
        return employeeRoleService.updateRole(roleId, request);
    }

    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long roleId) {
        employeeRoleService.deleteRole(roleId);
    }
}
