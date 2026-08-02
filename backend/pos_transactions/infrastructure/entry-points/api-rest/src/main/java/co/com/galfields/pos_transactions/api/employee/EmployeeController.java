package co.com.galfields.pos_transactions.api.employee;

import co.com.galfields.pos_transactions.model.employee.Employee;
import co.com.galfields.pos_transactions.usecase.employee.EmployeeUseCase;
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

/** Mirrors backend/pos's EmployeeController 1:1. */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeUseCase employeeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EmployeeResponse create(@RequestBody @Valid EmployeeRequest request) {
        return toResponse(employeeUseCase.create(toDomain(request), request.password()));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EmployeeResponse> list() {
        return employeeUseCase.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{employeeId}")
    @Transactional(readOnly = true)
    public EmployeeResponse get(@PathVariable("employeeId") Long employeeId) {
        return toResponse(employeeUseCase.get(employeeId));
    }

    @PutMapping("/{employeeId}")
    @Transactional
    public EmployeeResponse update(@PathVariable("employeeId") Long employeeId, @RequestBody @Valid EmployeeRequest request) {
        return toResponse(employeeUseCase.update(employeeId, toDomain(request), request.password()));
    }

    /** Soft-deactivate, not a hard delete — see EmployeeUseCase#deactivate. */
    @DeleteMapping("/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deactivate(@PathVariable("employeeId") Long employeeId) {
        employeeUseCase.deactivate(employeeId);
    }

    private Employee toDomain(EmployeeRequest request) {
        return Employee.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .roleId(request.roleId())
                .terminalIds(request.terminalIds())
                .active(true)
                .build();
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getEmployeeId(), employee.getFirstName(), employee.getLastName(), employee.getUsername(),
                employee.getRoleId(), employee.getRoleName(), employee.getTerminalIds(),
                employee.isActive(), employee.getCreatedAt());
    }
}
