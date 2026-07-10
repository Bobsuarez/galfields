package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.StockAdjustmentBatchRequest;
import co.com.galfields.pos.dto.StockAdjustmentBatchResponse;
import co.com.galfields.pos.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Batch stock adjustment reported by a POS terminal - one call per sale,
     * one item per line sold. Idempotent per (clientEventId, variantId): a
     * retried batch (e.g. the terminal applied locally but lost the
     * response) replays the same result instead of double-decrementing.
     */
    @PostMapping("/adjustments")
    public StockAdjustmentBatchResponse applyAdjustments(@RequestBody @Valid StockAdjustmentBatchRequest request) {
        return inventoryService.applyAdjustments(request);
    }
}
