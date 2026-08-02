package co.com.galfields.pos_transactions.api.inventory;

import co.com.galfields.pos_transactions.model.inventory.StockAdjustmentItem;
import co.com.galfields.pos_transactions.model.inventory.StockAdjustmentResult;
import co.com.galfields.pos_transactions.usecase.inventory.ApplyStockAdjustmentsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Mirrors backend/pos's InventoryController 1:1 — same path, same contract. */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ApplyStockAdjustmentsUseCase applyStockAdjustmentsUseCase;

    @PostMapping("/adjustments")
    @Transactional
    public StockAdjustmentBatchResponse applyAdjustments(@RequestBody @Valid StockAdjustmentBatchRequest request) {
        List<StockAdjustmentItem> items = request.items().stream()
                .map(item -> new StockAdjustmentItem(item.variantId(), item.quantityDelta()))
                .toList();

        List<StockAdjustmentResult> results = applyStockAdjustmentsUseCase.execute(request.clientEventId(), items);

        List<StockAdjustmentResultResponse> resultResponses = results.stream()
                .map(r -> new StockAdjustmentResultResponse(r.variantId(), r.alreadyProcessed(), r.resultingQuantity()))
                .toList();

        return new StockAdjustmentBatchResponse(request.clientEventId(), resultResponses);
    }
}
