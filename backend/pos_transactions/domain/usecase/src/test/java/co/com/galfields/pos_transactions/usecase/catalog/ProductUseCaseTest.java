package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.DuplicateResourceException;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.Brand;
import co.com.galfields.pos_transactions.model.catalog.Category;
import co.com.galfields.pos_transactions.model.catalog.Location;
import co.com.galfields.pos_transactions.model.catalog.Product;
import co.com.galfields.pos_transactions.model.catalog.ProductVariant;
import co.com.galfields.pos_transactions.model.catalog.gateways.BrandRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.CategoryRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageCompressionGateway;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageStorageGateway;
import co.com.galfields.pos_transactions.model.catalog.gateways.LocationCatalogRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.ProductRepository;
import co.com.galfields.pos_transactions.model.inventory.Inventory;
import co.com.galfields.pos_transactions.model.inventory.gateways.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private LocationCatalogRepository locationRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ImageCompressionGateway imageCompressionGateway;
    @Mock
    private ImageStorageGateway imageStorageGateway;

    private ProductUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ProductUseCase(productRepository, categoryRepository, brandRepository, locationRepository,
                inventoryRepository, imageCompressionGateway, imageStorageGateway);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().categoryId(1L).name("Bebidas").build()));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(Brand.builder().brandId(2L).name("Postobon").build()));
        when(locationRepository.findByName("Bogotá - Chapinero"))
                .thenReturn(Optional.of(Location.builder().locationId(10L).name("Bogotá - Chapinero").build()));
    }

    @Test
    void createValidatesCategoryAndBrandThenSavesAndSeedsStock() {
        ProductVariant variant = ProductVariant.builder().sku("SKU-1").barcode("BAR-1")
                .price(BigDecimal.TEN).costPrice(BigDecimal.ONE).initialStock(5).build();
        Product request = Product.builder().name("Coca Cola").categoryId(1L).brandId(2L)
                .variants(new ArrayList<>(List.of(variant))).build();

        when(productRepository.existsVariantWithSku("SKU-1", null)).thenReturn(false);
        when(productRepository.existsVariantWithBarcode("BAR-1", null)).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setProductId(100L);
            p.getVariants().forEach(v -> v.setVariantId(200L));
            return p;
        });
        when(inventoryRepository.findByVariantAndLocation(200L, 10L)).thenReturn(Optional.empty());

        Product result = useCase.create(request, null, java.util.Map.of());

        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getCategoryName()).isEqualTo("Bebidas");
        assertThat(result.getBrandName()).isEqualTo("Postobon");

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantityOnHand()).isEqualTo(5);
        assertThat(captor.getValue().getLocationId()).isEqualTo(10L);
    }

    @Test
    void createThrowsDuplicateWhenSkuRepeatedWithinRequest() {
        ProductVariant v1 = ProductVariant.builder().sku("SAME").barcode("B1").price(BigDecimal.ONE).costPrice(BigDecimal.ONE).build();
        ProductVariant v2 = ProductVariant.builder().sku("SAME").barcode("B2").price(BigDecimal.ONE).costPrice(BigDecimal.ONE).build();
        Product request = Product.builder().name("X").categoryId(1L).brandId(2L).variants(List.of(v1, v2)).build();

        assertThatThrownBy(() -> useCase.create(request, null, java.util.Map.of()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SAME");
        verify(productRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void createThrowsDuplicateWhenSkuAlreadyExistsInDb() {
        ProductVariant variant = ProductVariant.builder().sku("EXISTS").barcode("B1").price(BigDecimal.ONE).costPrice(BigDecimal.ONE).build();
        Product request = Product.builder().name("X").categoryId(1L).brandId(2L).variants(List.of(variant)).build();
        when(productRepository.existsVariantWithSku("EXISTS", null)).thenReturn(true);

        assertThatThrownBy(() -> useCase.create(request, null, java.util.Map.of()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createThrowsNotFoundWhenCategoryMissing() {
        Product request = Product.builder().name("X").categoryId(999L).brandId(2L).variants(List.of()).build();
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.create(request, null, java.util.Map.of()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUpsertsVariantBySku() {
        ProductVariant existingVariant = ProductVariant.builder().variantId(1L).sku("KEEP").barcode("B1")
                .price(BigDecimal.ONE).costPrice(BigDecimal.ONE).active(true).attributes(new ArrayList<>()).build();
        Product existing = Product.builder().productId(1L).name("Old").categoryId(1L).brandId(2L)
                .variants(new ArrayList<>(List.of(existingVariant))).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsVariantWithSku(any(), any())).thenReturn(false);
        when(productRepository.existsVariantWithBarcode(any(), any())).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.findByVariantAndLocation(any(), any())).thenReturn(Optional.empty());

        ProductVariant patch = ProductVariant.builder().sku("KEEP").barcode("B1-new")
                .price(new BigDecimal("99")).costPrice(BigDecimal.ONE).build();
        Product patchProduct = Product.builder().name("New Name").categoryId(1L).brandId(2L).build();

        Product result = useCase.update(1L, patchProduct, null, List.of(patch), null);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getVariants()).hasSize(1);
        assertThat(result.getVariants().getFirst().getVariantId()).isEqualTo(1L);
        assertThat(result.getVariants().getFirst().getBarcode()).isEqualTo("B1-new");
        assertThat(result.getVariants().getFirst().getPrice()).isEqualByComparingTo("99");
    }

    @Test
    void deactivateFlipsProductAndAllVariantsInactive() {
        ProductVariant variant = ProductVariant.builder().variantId(1L).sku("S").active(true).build();
        Product product = Product.builder().productId(1L).active(true).variants(new ArrayList<>(List.of(variant))).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        useCase.deactivate(1L);

        assertThat(product.isActive()).isFalse();
        assertThat(product.getVariants().getFirst().isActive()).isFalse();
    }
}
