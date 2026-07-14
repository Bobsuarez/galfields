package co.com.galfields.pos.repository;

import co.com.galfields.pos.dto.PaymentMethodSalesResponse;
import co.com.galfields.pos.entity.Payment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByTransaction_TransactionId(Long transactionId);

    @Query("""
            SELECT new co.com.galfields.pos.dto.PaymentMethodSalesResponse(
                pm.paymentMethodId, pm.methodName, SUM(p.amount), COUNT(p))
            FROM Payment p JOIN p.paymentMethod pm
            WHERE p.transaction.transactionDate BETWEEN :from AND :to
            GROUP BY pm.paymentMethodId, pm.methodName
            ORDER BY SUM(p.amount) DESC
            """)
    List<PaymentMethodSalesResponse> summarizeByPaymentMethod(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
