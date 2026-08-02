package co.com.galfields.pos_transactions.jpa.catalog;

import co.com.galfields.pos_transactions.model.catalog.Product;
import co.com.galfields.pos_transactions.model.catalog.ProductVariant;
import co.com.galfields.pos_transactions.model.catalog.VariantAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductRepositoryAdapterTest {

    @Mock
    private ProductJpaRepository productRepository;
    @Mock
    private ProductVariantJpaRepository variantRepository;
    @Mock
    private VariantAttributeJpaRepository attributeRepository;
    @Mock
    private ProductUnitJpaRepository unitRepository;
    @Mock
    private ProductImageJpaRepository productImageRepository;
    @Mock
    private ProductVariantImageJpaRepository variantImageRepository;
    @Mock
    private AttachFileJpaRepository attachFileRepository;

    private ProductRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new ProductRepositoryAdapter(productRepository, variantRepository, attributeRepository,
                unitRepository, productImageRepository, variantImageRepository, attachFileRepository);
    }

    @Test
    void saveInsertsProductThenVariantsThenAttributes() {
        when(productRepository.save(any())).thenAnswer(inv -> {
            ProductEntity e = inv.getArgument(0);
            e.setProductId(1L);
            return e;
        });
        when(variantRepository.save(any())).thenAnswer(inv -> {
            ProductVariantEntity e = inv.getArgument(0);
            e.setVariantId(10L);
            return e;
        });
        when(attributeRepository.findByVariantId(10L)).thenReturn(List.of());

        ProductVariant variant = ProductVariant.builder()
                .sku("SKU-1").barcode("B1").price(BigDecimal.TEN).costPrice(BigDecimal.ONE).active(true)
                .attributes(List.of(VariantAttribute.builder().attributeName("color").attributeValue("rojo").build()))
                .build();
        Product product = Product.builder().name("Test").categoryId(1L).brandId(2L).active(true)
                .variants(List.of(variant)).build();

        Product saved = adapter.save(product);

        assertThat(saved.getProductId()).isEqualTo(1L);
        assertThat(variant.getVariantId()).isEqualTo(10L);
        verify(attributeRepository).save(any());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(404L)).isEmpty();
    }

    @Test
    void existsVariantWithSkuUsesExcludeVariantWhenGiven() {
        adapter.existsVariantWithSku("SKU", 5L);
        verify(variantRepository).existsBySkuAndProductIdNot("SKU", 5L);

        adapter.existsVariantWithSku("SKU", null);
        verify(variantRepository).existsBySku("SKU");
    }
}
