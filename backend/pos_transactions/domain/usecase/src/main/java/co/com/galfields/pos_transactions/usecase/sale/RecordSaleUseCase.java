package co.com.galfields.pos_transactions.usecase.sale;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.sale.PaymentStatus;
import co.com.galfields.pos_transactions.model.sale.ProductUnitReference;
import co.com.galfields.pos_transactions.model.sale.Sale;
import co.com.galfields.pos_transactions.model.sale.StockDelta;
import co.com.galfields.pos_transactions.model.sale.gateways.EmployeeReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.LocationReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.PaymentMethodReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.ProductUnitReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.ProductVariantReferenceGateway;
import co.com.galfields.pos_transactions.model.sale.gateways.SaleRepository;
import co.com.galfields.pos_transactions.model.sale.gateways.StockGateway;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors backend/pos's SalesService#recordSale: idempotent by
 * clientEventId, writes the sale (transaction/items/payments) and decrements
 * stock atomically, sharing the same idempotency key end to end. Employee
 * attribution and location are placeholders (single seeded "pos-terminal"
 * employee, single default location) until real cashier login / multi
 * location exist — same as backend/pos today.
 */
@RequiredArgsConstructor
public class RecordSaleUseCase {

    private static final String DEFAULT_LOCATION_NAME = "Bogotá - Chapinero";
    private static final String DEFAULT_EMPLOYEE_USERNAME = "pos-terminal";

    private final SaleRepository saleRepository;
    private final EmployeeReferenceGateway employeeReferenceGateway;
    private final LocationReferenceGateway locationReferenceGateway;
    private final ProductVariantReferenceGateway productVariantReferenceGateway;
    private final ProductUnitReferenceGateway productUnitReferenceGateway;
    private final PaymentMethodReferenceGateway paymentMethodReferenceGateway;
    private final StockGateway stockGateway;

    public RecordSaleResult execute(Sale sale) {
        var existing = saleRepository.findByClientEventId(sale.getClientEventId());
        if (existing.isPresent()) {
            return new RecordSaleResult(existing.get(), true);
        }

        Long locationId = locationReferenceGateway.findIdByName(DEFAULT_LOCATION_NAME)
                .orElseThrow(() -> new ResourceNotFoundException("Default location '" + DEFAULT_LOCATION_NAME + "' not found"));
        Long employeeId = employeeReferenceGateway.findIdByUsername(DEFAULT_EMPLOYEE_USERNAME)
                .orElseThrow(() -> new ResourceNotFoundException("Default employee '" + DEFAULT_EMPLOYEE_USERNAME + "' not found"));

        List<StockDelta> deltas = sale.getItems().stream().map(item -> {
            if (!productVariantReferenceGateway.existsById(item.getVariantId())) {
                throw new ResourceNotFoundException("Variant " + item.getVariantId() + " not found");
            }

            String unitName = "Unidad";
            int conversionFactor = 1;
            if (item.getProductUnitId() != null) {
                ProductUnitReference unit = productUnitReferenceGateway
                        .find(item.getProductUnitId(), item.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product unit " + item.getProductUnitId() + " not found for variant " + item.getVariantId()));
                unitName = unit.getUnitName();
                conversionFactor = unit.getConversionFactor();
            }
            item.setUnitName(unitName);
            item.setConversionFactor(conversionFactor);
            item.setDiscountPerItem(BigDecimal.ZERO);

            return new StockDelta(item.getVariantId(), -item.getQuantity() * conversionFactor);
        }).toList();

        sale.getPayments().forEach(payment -> {
            if (!paymentMethodReferenceGateway.existsById(payment.getPaymentMethodId())) {
                throw new ResourceNotFoundException("Payment method " + payment.getPaymentMethodId() + " not found");
            }
        });

        sale.setEmployeeId(employeeId);
        sale.setLocationId(locationId);
        sale.setTaxAmount(BigDecimal.ZERO);
        sale.setPaymentStatus(PaymentStatus.Paid);

        Sale saved = saleRepository.save(sale);

        // Same clientEventId as the sale itself, same idempotent
        // decrement-by-base-units logic /api/inventory/adjustments will
        // expose publicly in Fase 3 — see StockGateway's JPA adapter.
        stockGateway.applyAdjustments(sale.getClientEventId(), locationId, deltas);

        return new RecordSaleResult(saved, false);
    }
}
