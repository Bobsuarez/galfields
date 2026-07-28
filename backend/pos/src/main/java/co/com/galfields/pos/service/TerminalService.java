package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.TerminalRequest;
import co.com.galfields.pos.dto.TerminalResponse;
import co.com.galfields.pos.entity.Terminal;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.TerminalRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TerminalService {

    private final TerminalRepository terminalRepository;

    @Transactional
    public TerminalResponse createTerminal(TerminalRequest request) {
        Terminal terminal = new Terminal();
        applyFields(terminal, request);
        return toResponse(terminalRepository.save(terminal));
    }

    @Transactional(readOnly = true)
    public TerminalResponse getTerminal(Long terminalId) {
        return toResponse(findOrThrow(terminalId));
    }

    @Transactional(readOnly = true)
    public List<TerminalResponse> listTerminals() {
        return terminalRepository.findAllByOrderByTerminalCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TerminalResponse updateTerminal(Long terminalId, TerminalRequest request) {
        Terminal terminal = findOrThrow(terminalId);
        applyFields(terminal, request);
        return toResponse(terminalRepository.save(terminal));
    }

    @Transactional
    public void deleteTerminal(Long terminalId) {
        Terminal terminal = findOrThrow(terminalId);
        terminalRepository.delete(terminal);
    }

    private Terminal findOrThrow(Long terminalId) {
        return terminalRepository.findById(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal " + terminalId + " not found"));
    }

    private void applyFields(Terminal terminal, TerminalRequest request) {
        terminal.setTerminalCode(request.terminalCode());
        terminal.setName(request.name());
        terminal.setActive(request.active());
    }

    private TerminalResponse toResponse(Terminal terminal) {
        return new TerminalResponse(
                terminal.getTerminalId(),
                terminal.getTerminalCode(),
                terminal.getName(),
                terminal.isActive(),
                terminal.getCreatedAt());
    }
}
