package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.catalog.CompressedImage;
import co.com.galfields.pos_transactions.model.catalog.PaymentMethod;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageCompressionGateway;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageStorageGateway;
import co.com.galfields.pos_transactions.model.catalog.gateways.PaymentMethodCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMethodUseCaseTest {

    @Mock
    private PaymentMethodCatalogRepository paymentMethodRepository;
    @Mock
    private ImageCompressionGateway imageCompressionGateway;
    @Mock
    private ImageStorageGateway imageStorageGateway;

    private PaymentMethodUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new PaymentMethodUseCase(paymentMethodRepository, imageCompressionGateway, imageStorageGateway);
    }

    @Test
    void createWithoutImageJustSaves() {
        PaymentMethod toSave = PaymentMethod.builder().methodName("Efectivo").active(true).build();
        PaymentMethod saved = PaymentMethod.builder().paymentMethodId(1L).methodName("Efectivo").active(true).build();
        when(paymentMethodRepository.save(toSave)).thenReturn(saved);

        PaymentMethod result = useCase.create(toSave, null);

        assertThat(result.getPaymentMethodId()).isEqualTo(1L);
        verify(imageCompressionGateway, org.mockito.Mockito.never()).compress(any(), any(), any());
    }

    @Test
    void createWithImageCompressesUploadsAndResolvesUrl() {
        PaymentMethod initial = PaymentMethod.builder().methodName("Nequi").active(true).build();
        PaymentMethod afterFirstSave = PaymentMethod.builder().paymentMethodId(2L).methodName("Nequi").active(true).build();
        when(paymentMethodRepository.save(initial)).thenReturn(afterFirstSave);

        CompressedImage compressed = new CompressedImage(new byte[]{1, 2, 3}, "image/webp", ".webp");
        when(imageCompressionGateway.compress(any(), any(), any())).thenReturn(compressed);
        when(imageStorageGateway.upload(eq("files/payment_method/nequi"), eq(compressed))).thenReturn("files/payment_method/nequi/uuid.webp");
        when(paymentMethodRepository.save(afterFirstSave)).thenReturn(afterFirstSave);
        when(imageStorageGateway.getPublicUrl("files/payment_method/nequi/uuid.webp")).thenReturn("https://cdn/files/payment_method/nequi/uuid.webp");

        PaymentMethod result = useCase.create(initial, new PaymentMethodUseCase.UploadedImage(new byte[]{9}, "image/png", "logo.png"));

        assertThat(result.getImageUrl()).isEqualTo("https://cdn/files/payment_method/nequi/uuid.webp");
        assertThat(afterFirstSave.getImageObjectKey()).isEqualTo("files/payment_method/nequi/uuid.webp");
    }

    @Test
    void deleteRemovesImageObjectWhenPresent() {
        PaymentMethod existing = PaymentMethod.builder().paymentMethodId(1L).imageObjectKey("files/payment_method/x/y.webp").build();
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(existing));

        useCase.delete(1L);

        verify(paymentMethodRepository).deleteById(1L);
        verify(imageStorageGateway).delete("files/payment_method/x/y.webp");
    }

    @Test
    void deleteSkipsStorageCallWhenNoImage() {
        PaymentMethod existing = PaymentMethod.builder().paymentMethodId(1L).build();
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(existing));

        useCase.delete(1L);

        verify(imageStorageGateway, org.mockito.Mockito.never()).delete(any());
    }
}
