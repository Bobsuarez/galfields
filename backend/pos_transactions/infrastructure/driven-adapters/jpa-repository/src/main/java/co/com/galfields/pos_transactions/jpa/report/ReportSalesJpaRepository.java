package co.com.galfields.pos_transactions.jpa.report;

import co.com.galfields.pos_transactions.jpa.sale.SalesTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Native SQL over sales_transactions/employees — entities here are flat with
 * no relations (see repo CLAUDE.md), so the JPQL path-navigation joins
 * backend/pos's ReportService relies on (t.employee.firstName, etc.) aren't
 * available; interface projections fill the same role as its constructor
 * expressions. Bound to SalesTransactionEntity purely as the repository's
 * type parameter — reuses Fase 2's real entity, doesn't modify it.
 */
public interface ReportSalesJpaRepository extends JpaRepository<SalesTransactionEntity, Long> {

    @Query(nativeQuery = true, value = """
            SELECT
                COALESCE(SUM(total_amount), 0) AS total_sales,
                COUNT(*) AS transaction_count,
                CASE WHEN COUNT(*) = 0 THEN 0 ELSE COALESCE(SUM(total_amount), 0) / COUNT(*) END AS average_ticket
            FROM sales_transactions
            WHERE transaction_date BETWEEN :from AND :to
              AND cancelled_at IS NULL
            """)
    SalesSummaryProjection summarize(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(nativeQuery = true, value = """
            SELECT
                t.transaction_id AS transaction_id,
                t.transaction_date AS transaction_date,
                e.first_name || ' ' || e.last_name AS employee_name,
                t.total_amount AS total_amount,
                t.discount_amount AS discount_amount,
                (SELECT COUNT(*) FROM sale_items si WHERE si.transaction_id = t.transaction_id) AS item_count,
                t.cancelled_at AS cancelled_at,
                t.invoice_prefix AS invoice_prefix,
                t.invoice_number AS invoice_number
            FROM sales_transactions t
            JOIN employees e ON e.employee_id = t.employee_id
            WHERE t.transaction_date BETWEEN :from AND :to
            ORDER BY t.transaction_date DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM sales_transactions t
            WHERE t.transaction_date BETWEEN :from AND :to
            """)
    Page<InvoiceSummaryProjection> findInvoiceSummaries(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT
                t.transaction_id AS transaction_id,
                t.transaction_date AS transaction_date,
                e.first_name || ' ' || e.last_name AS employee_name,
                t.total_amount AS total_amount,
                t.discount_amount AS discount_amount,
                t.tax_amount AS tax_amount,
                t.cancelled_at AS cancelled_at,
                t.invoice_prefix AS invoice_prefix,
                t.invoice_number AS invoice_number
            FROM sales_transactions t
            JOIN employees e ON e.employee_id = t.employee_id
            WHERE t.transaction_id = :transactionId
            """)
    Optional<InvoiceHeaderProjection> findInvoiceHeaderById(@Param("transactionId") Long transactionId);
}
