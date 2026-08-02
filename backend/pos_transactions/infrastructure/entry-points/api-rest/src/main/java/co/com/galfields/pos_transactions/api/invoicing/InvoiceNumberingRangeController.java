package co.com.galfields.pos_transactions.api.invoicing;

import co.com.galfields.pos_transactions.model.invoicing.InvoiceNumberingRange;
import co.com.galfields.pos_transactions.usecase.invoicing.InvoiceNumberingRangeUseCase;
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

/** Mirrors backend/pos's InvoiceNumberingRangeController 1:1. */
@RestController
@RequestMapping("/api/invoice-numbering-ranges")
@RequiredArgsConstructor
public class InvoiceNumberingRangeController {

    private final InvoiceNumberingRangeUseCase invoiceNumberingRangeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InvoiceNumberingRangeResponse create(@RequestBody @Valid InvoiceNumberingRangeRequest request) {
        return toResponse(invoiceNumberingRangeUseCase.create(toDomain(request)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<InvoiceNumberingRangeResponse> list() {
        return invoiceNumberingRangeUseCase.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{rangeId}")
    @Transactional(readOnly = true)
    public InvoiceNumberingRangeResponse get(@PathVariable("rangeId") Long rangeId) {
        return toResponse(invoiceNumberingRangeUseCase.get(rangeId));
    }

    /** Used by the desktop POS terminal to pull its own assigned range
     * without needing to know its own numeric rangeId. */
    @GetMapping("/by-terminal/{terminalCode}")
    @Transactional(readOnly = true)
    public InvoiceNumberingRangeResponse getByTerminal(@PathVariable("terminalCode") String terminalCode) {
        return toResponse(invoiceNumberingRangeUseCase.getByTerminalCode(terminalCode));
    }

    @PutMapping("/{rangeId}")
    @Transactional
    public InvoiceNumberingRangeResponse update(@PathVariable("rangeId") Long rangeId, @RequestBody @Valid InvoiceNumberingRangeRequest request) {
        return toResponse(invoiceNumberingRangeUseCase.update(rangeId, toDomain(request)));
    }

    @DeleteMapping("/{rangeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("rangeId") Long rangeId) {
        invoiceNumberingRangeUseCase.delete(rangeId);
    }

    private InvoiceNumberingRange toDomain(InvoiceNumberingRangeRequest request) {
        return InvoiceNumberingRange.builder()
                .terminalId(request.terminalId())
                .prefix(request.prefix())
                .rangeStart(request.rangeStart())
                .rangeEnd(request.rangeEnd())
                .build();
    }

    private InvoiceNumberingRangeResponse toResponse(InvoiceNumberingRange range) {
        return new InvoiceNumberingRangeResponse(range.getRangeId(), range.getTerminalId(), range.getTerminalCode(),
                range.getPrefix(), range.getRangeStart(), range.getRangeEnd(), range.getCreatedAt(), range.getUpdatedAt());
    }
}
