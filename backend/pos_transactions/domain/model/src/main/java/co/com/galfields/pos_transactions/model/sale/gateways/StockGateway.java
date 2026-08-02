package co.com.galfields.pos_transactions.model.sale.gateways;

import co.com.galfields.pos_transactions.model.sale.StockAdjustmentOutcome;
import co.com.galfields.pos_transactions.model.sale.StockDelta;

import java.util.List;

public interface StockGateway {
    List<StockAdjustmentOutcome> applyAdjustments(String clientEventId, Long locationId, List<StockDelta> deltas);
}
