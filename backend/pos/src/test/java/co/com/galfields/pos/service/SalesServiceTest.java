package co.com.galfields.pos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.galfields.pos.dto.SaleLineRequest;
import co.com.galfields.pos.dto.SalePaymentRequest;
import co.com.galfields.pos.dto.SaleRequest;
import co.com.galfields.pos.entity.Employee;
import co.com.galfields.pos.entity.Location;
import co.com.galfields.pos.entity.PaymentMethod;
import co.com.galfields.pos.entity.ProductVariant;
import co.com.galfields.pos.entity.SalesTransaction;
import co.com.galfields.pos.repository.EmployeeRepository;
import co.com.galfields.pos.repository.LocationRepository;
import co.com.galfields.pos.repository.PaymentMethodRepository;
import co.com.galfields.pos.repository.PaymentRepository;
import co.com.galfields.pos.repository.ProductVariantRepository;
import co.com.galfields.pos.repository.SaleItemRepository;
import co.com.galfields.pos.repository.SalesTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers specs/01-reportes-mobile-pos-zona-horaria.md's second half: the
 * cloud must record the sale's real timestamp when the terminal sends one,
 * and keep falling back to "moment received" for terminals that don't yet.
 */
@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    private SalesTransactionRepository salesTransactionRepository;
    @Mock
    private SaleItemRepository saleItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private InventoryService inventoryService;

    private SalesService salesService;

    @BeforeEach
    void setUp() {
        salesService = new SalesService(
                salesTransactionRepository,
                saleItemRepository,
                paymentRepository,
                employeeRepository,
                locationRepository,
                productVariantRepository,
                paymentMethodRepository,
                inventoryService);

        when(salesTransactionRepository.findByClientEventId(any())).thenReturn(Optional.empty());
        when(locationRepository.findByName(any())).thenReturn(Optional.of(new Location()));
        when(employeeRepository.findByUsername(any())).thenReturn(Optional.of(new Employee()));
        when(productVariantRepository.findById(any())).thenReturn(Optional.of(new ProductVariant()));
        when(paymentMethodRepository.findById(any())).thenReturn(Optional.of(new PaymentMethod()));
    }

    private SaleRequest requestWithTransactionDate(OffsetDateTime transactionDate) {
        return new SaleRequest(
                "test-event-" + transactionDate,
                List.of(new SaleLineRequest(1L, 1, BigDecimal.TEN, BigDecimal.TEN)),
                List.of(new SalePaymentRequest(1L, BigDecimal.TEN, null)),
                BigDecimal.ZERO,
                BigDecimal.TEN,
                null,
                null,
                transactionDate);
    }

    @Test
    void usesTheTerminalsTransactionDateWhenPresent() {
        // 19:08 Bogotá on 2026-08-03 — the real case from the spec.
        OffsetDateTime bogotaSaleTime = OffsetDateTime.of(2026, 8, 3, 19, 8, 29, 0, ZoneOffset.ofHours(-5));

        salesService.recordSale(requestWithTransactionDate(bogotaSaleTime));

        ArgumentCaptor<SalesTransaction> captor = ArgumentCaptor.forClass(SalesTransaction.class);
        verify(salesTransactionRepository).save(captor.capture());

        assertThat(captor.getValue().getTransactionDate()).isEqualTo(LocalDateTime.of(2026, 8, 4, 0, 8, 29));
    }

    @Test
    void fallsBackToReceiptTimeWhenTransactionDateIsAbsent() {
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

        salesService.recordSale(requestWithTransactionDate(null));

        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);

        ArgumentCaptor<SalesTransaction> captor = ArgumentCaptor.forClass(SalesTransaction.class);
        verify(salesTransactionRepository).save(captor.capture());

        LocalDateTime stored = captor.getValue().getTransactionDate();
        assertThat(stored).isNotNull();
        assertThat(stored).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}
