package co.com.galfields.pos_transactions.usecase.sale;

import co.com.galfields.pos_transactions.model.InvalidStateException;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.sale.Sale;
import co.com.galfields.pos_transactions.model.sale.StockDelta;
import co.com.galfields.pos_transactions.model.sale.gateways.SaleRepository;
import co.com.galfields.pos_transactions.model.sale.gateways.StockGateway;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mirrors backend/pos's SalesService#cancel: reverses the stock decrement
 * recordSale applied and marks the sale cancelled, atomically. Two entry
 * points (transactionId / clientEventId) because mobile and the desktop POS
 * each only know one of the two identifiers.
 */
@RequiredArgsConstructor
public class CancelSaleUseCase {

    private final SaleRepository saleRepository;
    private final StockGateway stockGateway;

    public void cancelByTransactionId(Long transactionId) {
        Sale sale = saleRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction " + transactionId + " not found"));
        cancel(sale);
    }

    public void cancelByClientEventId(String clientEventId) {
        Sale sale = saleRepository.findByClientEventId(clientEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale with clientEventId " + clientEventId + " not found"));
        cancel(sale);
    }

    private void cancel(Sale sale) {
        if (sale.getCancelledAt() != null) {
            throw new InvalidStateException("Esta factura ya está cancelada");
        }

        // Must use a different clientEventId than the original sale — the
        // stock adjustment idempotency key is (clientEventId, variantId), so
        // reusing the same one would look "already processed" and the
        // reversal would silently never apply. See RecordSaleUseCase.
        var reversalDeltas = sale.getItems().stream()
                .map(item -> new StockDelta(item.getVariantId(), item.getQuantity() * item.getConversionFactor()))
                .toList();
        stockGateway.applyAdjustments("cancel-" + sale.getClientEventId(), sale.getLocationId(), reversalDeltas);

        sale.setCancelledAt(LocalDateTime.now());
        saleRepository.save(sale);
    }
}
