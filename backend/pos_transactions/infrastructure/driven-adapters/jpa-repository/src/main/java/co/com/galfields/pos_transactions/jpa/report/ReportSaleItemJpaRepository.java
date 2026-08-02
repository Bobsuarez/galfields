package co.com.galfields.pos_transactions.jpa.report;

import co.com.galfields.pos_transactions.jpa.sale.SaleItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportSaleItemJpaRepository extends JpaRepository<SaleItemEntity, Long> {

    @Query(nativeQuery = true, value = """
            SELECT
                p.name AS product_name,
                v.sku AS sku,
                si.quantity AS quantity,
                si.unit_price AS unit_price,
                si.subtotal AS subtotal,
                si.unit_name AS unit_name,
                si.conversion_factor AS conversion_factor
            FROM sale_items si
            JOIN product_variants v ON v.variant_id = si.variant_id
            JOIN products p ON p.product_id = v.product_id
            WHERE si.transaction_id = :transactionId
            """)
    List<InvoiceLineProjection> findLinesByTransactionId(@Param("transactionId") Long transactionId);
}
