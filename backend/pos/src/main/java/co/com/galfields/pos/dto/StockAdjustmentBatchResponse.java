package co.com.galfields.pos.dto;

import java.util.List;

public record StockAdjustmentBatchResponse(
        String clientEventId,
        List<StockAdjustmentResult> results) {
}
