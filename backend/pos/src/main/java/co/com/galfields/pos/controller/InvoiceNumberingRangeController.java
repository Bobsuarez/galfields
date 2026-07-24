package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.InvoiceNumberingRangeRequest;
import co.com.galfields.pos.dto.InvoiceNumberingRangeResponse;
import co.com.galfields.pos.service.InvoiceNumberingRangeService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/invoice-numbering-ranges")
@RequiredArgsConstructor
public class InvoiceNumberingRangeController {

    private final InvoiceNumberingRangeService invoiceNumberingRangeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceNumberingRangeResponse create(@RequestBody @Valid InvoiceNumberingRangeRequest request) {
        return invoiceNumberingRangeService.createRange(request);
    }

    @GetMapping
    public List<InvoiceNumberingRangeResponse> list() {
        return invoiceNumberingRangeService.listRanges();
    }

    @GetMapping("/{rangeId}")
    public InvoiceNumberingRangeResponse get(@PathVariable Long rangeId) {
        return invoiceNumberingRangeService.getRange(rangeId);
    }

    // Used by the desktop POS terminal to pull its own assigned range
    // (see apps/galfield-pos's invoice_numbering.rs) without needing to
    // know its own numeric rangeId.
    @GetMapping("/by-terminal/{terminalCode}")
    public InvoiceNumberingRangeResponse getByTerminal(@PathVariable String terminalCode) {
        return invoiceNumberingRangeService.getRangeByTerminalCode(terminalCode);
    }

    @PutMapping("/{rangeId}")
    public InvoiceNumberingRangeResponse update(
            @PathVariable Long rangeId,
            @RequestBody @Valid InvoiceNumberingRangeRequest request
    ) {
        return invoiceNumberingRangeService.updateRange(rangeId, request);
    }

    @DeleteMapping("/{rangeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long rangeId) {
        invoiceNumberingRangeService.deleteRange(rangeId);
    }
}
