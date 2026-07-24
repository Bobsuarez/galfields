package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.InvoiceNumberingRangeRequest;
import co.com.galfields.pos.dto.InvoiceNumberingRangeResponse;
import co.com.galfields.pos.entity.InvoiceNumberingRange;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.InvoiceNumberingRangeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceNumberingRangeService {

    private final InvoiceNumberingRangeRepository invoiceNumberingRangeRepository;

    @Transactional
    public InvoiceNumberingRangeResponse createRange(InvoiceNumberingRangeRequest request) {
        InvoiceNumberingRange range = new InvoiceNumberingRange();
        applyFields(range, request);
        return toResponse(invoiceNumberingRangeRepository.save(range));
    }

    @Transactional(readOnly = true)
    public InvoiceNumberingRangeResponse getRange(Long rangeId) {
        return toResponse(findOrThrow(rangeId));
    }

    @Transactional(readOnly = true)
    public List<InvoiceNumberingRangeResponse> listRanges() {
        return invoiceNumberingRangeRepository.findAllByOrderByTerminalCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceNumberingRangeResponse getRangeByTerminalCode(String terminalCode) {
        return invoiceNumberingRangeRepository.findByTerminalCode(terminalCode)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay rango de facturación asignado para la terminal '" + terminalCode + "'"));
    }

    @Transactional
    public InvoiceNumberingRangeResponse updateRange(Long rangeId, InvoiceNumberingRangeRequest request) {
        InvoiceNumberingRange range = findOrThrow(rangeId);
        applyFields(range, request);
        return toResponse(invoiceNumberingRangeRepository.save(range));
    }

    @Transactional
    public void deleteRange(Long rangeId) {
        InvoiceNumberingRange range = findOrThrow(rangeId);
        invoiceNumberingRangeRepository.delete(range);
    }

    private InvoiceNumberingRange findOrThrow(Long rangeId) {
        return invoiceNumberingRangeRepository.findById(rangeId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice numbering range " + rangeId + " not found"));
    }

    private void applyFields(InvoiceNumberingRange range, InvoiceNumberingRangeRequest request) {
        range.setTerminalCode(request.terminalCode());
        range.setPrefix(request.prefix());
        range.setRangeStart(request.rangeStart());
        range.setRangeEnd(request.rangeEnd());
    }

    private InvoiceNumberingRangeResponse toResponse(InvoiceNumberingRange range) {
        return new InvoiceNumberingRangeResponse(
                range.getRangeId(),
                range.getTerminalCode(),
                range.getPrefix(),
                range.getRangeStart(),
                range.getRangeEnd(),
                range.getCreatedAt(),
                range.getUpdatedAt());
    }
}
