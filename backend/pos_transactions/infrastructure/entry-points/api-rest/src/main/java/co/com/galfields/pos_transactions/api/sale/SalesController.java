package co.com.galfields.pos_transactions.api.sale;

import co.com.galfields.pos_transactions.model.sale.Payment;
import co.com.galfields.pos_transactions.model.sale.Sale;
import co.com.galfields.pos_transactions.model.sale.SaleItem;
import co.com.galfields.pos_transactions.usecase.sale.CancelSaleUseCase;
import co.com.galfields.pos_transactions.usecase.sale.RecordSaleResult;
import co.com.galfields.pos_transactions.usecase.sale.RecordSaleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors backend/pos's SalesController 1:1 — same paths, same contract.
 * Transaction boundary lives here (not in the usecase layer, which stays
 * framework-free per Clean Architecture) — @Transactional on a Spring bean
 * method wraps the whole synchronous call graph underneath it, including
 * the usecase's calls into SaleRepository/StockGateway, so atomicity is
 * unaffected by where the annotation sits.
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final RecordSaleUseCase recordSaleUseCase;
    private final CancelSaleUseCase cancelSaleUseCase;

    @PostMapping
    @Transactional
    public SaleResponse recordSale(@RequestBody @Valid SaleRequest request) {
        RecordSaleResult result = recordSaleUseCase.execute(toDomain(request));
        return new SaleResponse(result.sale().getTransactionId(), request.clientEventId(), result.alreadyProcessed());
    }

    /** Used by mobile's Historial de facturas, which already has the transactionId. */
    @PostMapping("/{transactionId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void cancel(@PathVariable("transactionId") Long transactionId) {
        cancelSaleUseCase.cancelByTransactionId(transactionId);
    }

    /** Used by the desktop POS, which only ever knows its own clientEventId (sync_uuid). */
    @PostMapping("/by-client-event/{clientEventId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void cancelByClientEvent(@PathVariable("clientEventId") String clientEventId) {
        cancelSaleUseCase.cancelByClientEventId(clientEventId);
    }

    private Sale toDomain(SaleRequest request) {
        List<SaleItem> items = request.items().stream()
                .map(line -> SaleItem.builder()
                        .variantId(line.variantId())
                        .productUnitId(line.productUnitId())
                        .quantity(line.quantity())
                        .unitPrice(line.unitPrice())
                        .subtotal(line.subtotal())
                        .discountPerItem(BigDecimal.ZERO)
                        .build())
                .toList();

        List<Payment> payments = request.payments().stream()
                .map(p -> Payment.builder()
                        .paymentMethodId(p.paymentMethodId())
                        .amount(p.amount())
                        .referenceNumber(p.referenceNumber())
                        .build())
                .toList();

        return Sale.builder()
                .clientEventId(request.clientEventId())
                .discountAmount(request.discountAmount())
                .totalAmount(request.totalAmount())
                .invoicePrefix(request.invoicePrefix())
                .invoiceNumber(request.invoiceNumber())
                .items(items)
                .payments(payments)
                .build();
    }
}
