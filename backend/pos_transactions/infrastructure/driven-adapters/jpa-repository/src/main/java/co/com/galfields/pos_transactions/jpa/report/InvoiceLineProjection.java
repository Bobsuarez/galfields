package co.com.galfields.pos_transactions.jpa.report;

import java.math.BigDecimal;

public interface InvoiceLineProjection {
    String getProductName();

    String getSku();

    Integer getQuantity();

    BigDecimal getUnitPrice();

    BigDecimal getSubtotal();

    String getUnitName();

    Integer getConversionFactor();
}
