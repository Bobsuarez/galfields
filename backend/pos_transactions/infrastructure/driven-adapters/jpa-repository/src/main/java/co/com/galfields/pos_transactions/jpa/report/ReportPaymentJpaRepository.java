package co.com.galfields.pos_transactions.jpa.report;

import co.com.galfields.pos_transactions.jpa.sale.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportPaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    @Query(nativeQuery = true, value = """
            SELECT
                pm.payment_method_id AS payment_method_id,
                pm.method_name AS method_name,
                SUM(p.amount) AS total_amount,
                COUNT(*) AS transaction_count
            FROM payments p
            JOIN payment_methods pm ON pm.payment_method_id = p.payment_method_id
            JOIN sales_transactions t ON t.transaction_id = p.transaction_id
            WHERE t.transaction_date BETWEEN :from AND :to
              AND t.cancelled_at IS NULL
            GROUP BY pm.payment_method_id, pm.method_name
            ORDER BY SUM(p.amount) DESC
            """)
    List<PaymentMethodSalesProjection> summarizeByPaymentMethod(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(nativeQuery = true, value = """
            SELECT
                pm.method_name AS method_name,
                p.amount AS amount,
                p.reference_number AS reference_number
            FROM payments p
            JOIN payment_methods pm ON pm.payment_method_id = p.payment_method_id
            WHERE p.transaction_id = :transactionId
            """)
    List<InvoicePaymentProjection> findPaymentsByTransactionId(@Param("transactionId") Long transactionId);
}
