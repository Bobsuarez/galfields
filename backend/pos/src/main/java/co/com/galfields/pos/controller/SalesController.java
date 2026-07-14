package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.SaleRequest;
import co.com.galfields.pos.dto.SaleResponse;
import co.com.galfields.pos.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
