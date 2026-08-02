package co.com.galfields.pos_transactions.api.inventory;

import java.util.List;

public record StockAdjustmentBatchResponse(
        String clientEventId,
        List<StockAdjustmentResultResponse> results) {
}
