package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.SaleRequest;
import co.com.galfields.pos.dto.SaleResponse;
import co.com.galfields.pos.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    /**
     * Reports one completed sale from a POS terminal — records the
     * transaction/items/payments and applies the matching stock adjustment
     * atomically. Idempotent by {@code clientEventId}; see SalesService.
     */
    @PostMapping
    public SaleResponse recordSale(@RequestBody @Valid SaleRequest request) {
        return salesService.recordSale(request);
    }

    /** Used by mobile's Historial de facturas, which already has the transactionId. */
    @PostMapping("/{transactionId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long transactionId) {
        salesService.cancelSale(transactionId);
    }

    /** Used by the desktop POS, which only ever knows its own clientEventId
     * (sync_uuid) — see SalesService#cancelSaleByClientEventId. */
    @PostMapping("/by-client-event/{clientEventId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelByClientEvent(@PathVariable String clientEventId) {
        salesService.cancelSaleByClientEventId(clientEventId);
    }
}
