package co.com.galfields.pos.repository;

import co.com.galfields.pos.dto.InvoiceSummaryResponse;
import co.com.galfields.pos.dto.SalesSummaryResponse;
import co.com.galfields.pos.entity.SalesTransaction;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {

    Optional<SalesTransaction> findByClientEventId(String clientEventId);

    @Query("""
            SELECT new co.com.galfields.pos.dto.SalesSummaryResponse(
                COALESCE(SUM(t.totalAmount), 0),
                COUNT(t),
                CASE WHEN COUNT(t) = 0 THEN 0 ELSE COALESCE(SUM(t.totalAmount), 0) / COUNT(t) END)
            FROM SalesTransaction t
            WHERE t.transactionDate BETWEEN :from AND :to
            """)
    SalesSummaryResponse summarize(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT new co.com.galfields.pos.dto.InvoiceSummaryResponse(
                t.transactionId, t.transactionDate, CONCAT(t.employee.firstName, ' ', t.employee.lastName),
                t.totalAmount, t.discountAmount,
                (SELECT COUNT(si) FROM SaleItem si WHERE si.transaction = t))
            FROM SalesTransaction t
            WHERE t.transactionDate BETWEEN :from AND :to
            ORDER BY t.transactionDate DESC
            """)
    Page<InvoiceSummaryResponse> findInvoiceSummaries(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
