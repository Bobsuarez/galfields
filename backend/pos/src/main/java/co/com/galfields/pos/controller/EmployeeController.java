package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.EmployeeRequest;
import co.com.galfields.pos.dto.EmployeeResponse;
import co.com.galfields.pos.service.EmployeeService;
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
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@RequestBody @Valid EmployeeRequest request) {
        return employeeService.createEmployee(request);
    }

    @GetMapping
    public List<EmployeeResponse> list() {
        return employeeService.listEmployees();
    }

    @GetMapping("/{employeeId}")
    public EmployeeResponse get(@PathVariable Long employeeId) {
        return employeeService.getEmployee(employeeId);
    }

    @PutMapping("/{employeeId}")
    public EmployeeResponse update(
            @PathVariable Long employeeId,
            @RequestBody @Valid EmployeeRequest request
    ) {
        return employeeService.updateEmployee(employeeId, request);
    }

    // Soft-deactivate, not a hard delete - see EmployeeService#deactivateEmployee.
    @DeleteMapping("/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long employeeId) {
        employeeService.deactivateEmployee(employeeId);
    }
}
