package co.com.galfields.pos_transactions.usecase.invoicing;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.employee.Terminal;
import co.com.galfields.pos_transactions.model.employee.gateways.TerminalRepository;
import co.com.galfields.pos_transactions.model.invoicing.InvoiceNumberingRange;
import co.com.galfields.pos_transactions.model.invoicing.gateways.InvoiceNumberingRangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InvoiceNumberingRangeUseCaseTest {

    @Mock
    private InvoiceNumberingRangeRepository rangeRepository;
    @Mock
    private TerminalRepository terminalRepository;

    private InvoiceNumberingRangeUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new InvoiceNumberingRangeUseCase(rangeRepository, terminalRepository);
    }

    @Test
    void createResolvesTerminalCodeBeforeSaving() {
        when(terminalRepository.findById(10L)).thenReturn(Optional.of(Terminal.builder().terminalId(10L).terminalCode("T1").build()));
        when(rangeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceNumberingRange range = InvoiceNumberingRange.builder().terminalId(10L).prefix("FE").rangeStart(1L).rangeEnd(1000L).build();
        InvoiceNumberingRange result = useCase.create(range);

        assertThat(result.getTerminalCode()).isEqualTo("T1");
    }

    @Test
    void createThrowsNotFoundWhenTerminalMissing() {
        when(terminalRepository.findById(999L)).thenReturn(Optional.empty());
        InvoiceNumberingRange range = InvoiceNumberingRange.builder().terminalId(999L).build();

        assertThatThrownBy(() -> useCase.create(range)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByTerminalCodeThrowsNotFoundWhenTerminalUnknown() {
        when(terminalRepository.findByCode("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getByTerminalCode("GHOST"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GHOST");
    }

    @Test
    void getByTerminalCodeThrowsNotFoundWhenTerminalHasNoRangeYet() {
        when(terminalRepository.findByCode("T1")).thenReturn(Optional.of(Terminal.builder().terminalId(10L).terminalCode("T1").build()));
        when(rangeRepository.findByTerminalId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getByTerminalCode("T1")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByTerminalCodeReturnsAssignedRange() {
        when(terminalRepository.findByCode("T1")).thenReturn(Optional.of(Terminal.builder().terminalId(10L).terminalCode("T1").build()));
        InvoiceNumberingRange range = InvoiceNumberingRange.builder().rangeId(1L).terminalId(10L).prefix("FE").rangeStart(1L).rangeEnd(1000L).build();
        when(rangeRepository.findByTerminalId(10L)).thenReturn(Optional.of(range));

        assertThat(useCase.getByTerminalCode("T1")).isEqualTo(range);
    }

    @Test
    void deleteChecksExistenceFirst() {
        when(rangeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
