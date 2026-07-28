package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.TerminalRequest;
import co.com.galfields.pos.dto.TerminalResponse;
import co.com.galfields.pos.service.TerminalService;
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
@RequestMapping("/api/terminals")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TerminalResponse create(@RequestBody @Valid TerminalRequest request) {
        return terminalService.createTerminal(request);
    }

    @GetMapping
    public List<TerminalResponse> list() {
        return terminalService.listTerminals();
    }

    @GetMapping("/{terminalId}")
    public TerminalResponse get(@PathVariable Long terminalId) {
        return terminalService.getTerminal(terminalId);
    }

    @PutMapping("/{terminalId}")
    public TerminalResponse update(
            @PathVariable Long terminalId,
            @RequestBody @Valid TerminalRequest request
    ) {
        return terminalService.updateTerminal(terminalId, request);
    }

    @DeleteMapping("/{terminalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long terminalId) {
        terminalService.deleteTerminal(terminalId);
    }
}
