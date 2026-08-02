package co.com.galfields.pos_transactions.usecase.reportsaccess;

import co.com.galfields.pos_transactions.model.reportsaccess.ReportsAccessCode;
import co.com.galfields.pos_transactions.model.reportsaccess.gateways.ReportsAccessCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportsAccessCodeUseCaseTest {

    @Mock
    private ReportsAccessCodeRepository repository;

    private ReportsAccessCodeUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ReportsAccessCodeUseCase(repository);
    }

    @Test
    void generateCreatesSixDigitZeroPaddedCode() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportsAccessCode result = useCase.generate();

        assertThat(result.getCode()).hasSize(6);
        assertThat(result.getCode()).matches("\\d{6}");
        assertThat(result.getGeneratedAt()).isNotNull();

        ArgumentCaptor<ReportsAccessCode> captor = ArgumentCaptor.forClass(ReportsAccessCode.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(result.getCode());
    }

    @Test
    void validateReturnsTrueWhenCodeMatchesLatest() {
        when(repository.findLatest()).thenReturn(Optional.of(
                ReportsAccessCode.builder().code("123456").generatedAt(LocalDateTime.now()).build()));

        assertThat(useCase.validate("123456")).isTrue();
    }

    @Test
    void validateReturnsFalseWhenCodeDoesNotMatchLatest() {
        when(repository.findLatest()).thenReturn(Optional.of(
                ReportsAccessCode.builder().code("123456").generatedAt(LocalDateTime.now()).build()));

        assertThat(useCase.validate("000000")).isFalse();
    }

    @Test
    void validateReturnsFalseWhenNoCodeEverGenerated() {
        when(repository.findLatest()).thenReturn(Optional.empty());

        assertThat(useCase.validate("123456")).isFalse();
    }
}
