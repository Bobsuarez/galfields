package co.com.galfields.pos_transactions.api.employee;

import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.usecase.employee.TerminalUseCase;
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

/** Mirrors backend/pos's TerminalController 1:1. */
@RestController
@RequestMapping("/api/terminals")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalUseCase terminalUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public TerminalResponse create(@RequestBody @Valid TerminalRequest request) {
        return toResponse(terminalUseCase.create(toDomain(request)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<TerminalResponse> list() {
        return terminalUseCase.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{terminalId}")
    @Transactional(readOnly = true)
    public TerminalResponse get(@PathVariable("terminalId") Long terminalId) {
        return toResponse(terminalUseCase.get(terminalId));
    }

    @PutMapping("/{terminalId}")
    @Transactional
    public TerminalResponse update(@PathVariable("terminalId") Long terminalId, @RequestBody @Valid TerminalRequest request) {
        return toResponse(terminalUseCase.update(terminalId, toDomain(request)));
    }

    @DeleteMapping("/{terminalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("terminalId") Long terminalId) {
        terminalUseCase.delete(terminalId);
    }

    private Terminal toDomain(TerminalRequest request) {
        return Terminal.builder().terminalCode(request.terminalCode()).name(request.name()).active(request.active()).build();
    }

    private TerminalResponse toResponse(Terminal terminal) {
        return new TerminalResponse(terminal.getTerminalId(), terminal.getTerminalCode(), terminal.getName(),
                terminal.isActive(), terminal.getCreatedAt());
    }
}
