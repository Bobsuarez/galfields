package co.com.galfields.pos_transactions.jpa.catalog;

import co.com.galfields.pos_transactions.model.PageQuery;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.catalog.Product;
import co.com.galfields.pos_transactions.model.catalog.ProductUnit;
import co.com.galfields.pos_transactions.model.catalog.ProductVariant;
import co.com.galfields.pos_transactions.model.catalog.VariantAttribute;
import co.com.galfields.pos_transactions.model.catalog.gateways.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productRepository;
    private final ProductVariantJpaRepository variantRepository;
    private final VariantAttributeJpaRepository attributeRepository;
    private final ProductUnitJpaRepository unitRepository;
    private final ProductImageJpaRepository productImageRepository;
    private final ProductVariantImageJpaRepository variantImageRepository;
    private final AttachFileJpaRepository attachFileRepository;

    @Override
    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId).map(this::toDomain);
    }

    @Override
    public PageResult<Product> findPage(PageQuery query, boolean includeInactive) {
        Sort sort = query.orders().isEmpty()
                ? Sort.unsorted()
                : Sort.by(query.orders().stream()
                        .map(o -> o.ascending() ? Sort.Order.asc(o.property()) : Sort.Order.desc(o.property()))
                        .toList());
        var pageable = PageRequest.of(query.page(), query.size(), sort);

        var page = includeInactive ? productRepository.findAll(pageable) : productRepository.findByActiveTrue(pageable);

        return new PageResult<>(
                page.getContent().stream().map(this::toDomain).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setProductId(product.getProductId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setCategoryId(product.getCategoryId());
        entity.setBrandId(product.getBrandId());
        entity.setActive(product.isActive());
        // See CategoryRepositoryAdapter's comment on createdAt — matters
        // here specifically because create()/update() with an image do a
        // second save() call on an already-persisted Product.
        entity.setCreatedAt(product.getCreatedAt());
        ProductEntity savedProduct = productRepository.save(entity);

        if (product.getImageMimeType() != null) {
            attachProductImage(savedProduct.getProductId(), product);
        }

        for (ProductVariant variant : product.getVariants()) {
            saveVariant(savedProduct.getProductId(), variant);
        }

        Product result = toDomain(savedProduct);
        // categoryName/brandName aren't columns — they're not something
        // toDomain(ProductEntity) can resolve on its own; carry forward
        // what the usecase already resolved via CategoryRepository/BrandRepository.
        result.setCategoryName(product.getCategoryName());
        result.setBrandName(product.getBrandName());
        return result;
    }

    private void saveVariant(Long productId, ProductVariant variant) {
        ProductVariantEntity entity = new ProductVariantEntity();
        entity.setVariantId(variant.getVariantId());
        entity.setProductId(productId);
        entity.setSku(variant.getSku());
        entity.setBarcode(variant.getBarcode());
        entity.setPrice(variant.getPrice());
        entity.setCostPrice(variant.getCostPrice());
        entity.setActive(variant.isActive());
        ProductVariantEntity savedVariant = variantRepository.save(entity);
        variant.setVariantId(savedVariant.getVariantId());

        if (variant.getAttributes() != null) {
            List<VariantAttributeEntity> existingAttributes = attributeRepository.findByVariantId(savedVariant.getVariantId());
            if (!existingAttributes.isEmpty()) {
                attributeRepository.deleteAll(existingAttributes);
                // Must hit the DB before the inserts below: Hibernate's flush
                // order processes inserts before deletes within a single
                // flush, so without this, re-inserting the same
                // (variant_id, attribute_name) pair being deleted here
                // trips the unique constraint — same root cause as
                // backend/pos's orphanRemoval flush-ordering bug (see
                // ProductService#applyVariantAttributes there), different
                // code path since this adapter uses plain repository calls
                // instead of a JPA cascade relation.
                attributeRepository.flush();
            }
            for (VariantAttribute attribute : variant.getAttributes()) {
                VariantAttributeEntity attributeEntity = new VariantAttributeEntity();
                attributeEntity.setVariantId(savedVariant.getVariantId());
                attributeEntity.setAttributeName(attribute.getAttributeName());
                attributeEntity.setAttributeValue(attribute.getAttributeValue());
                attributeRepository.save(attributeEntity);
            }
        }

        if (variant.getImageMimeType() != null) {
            attachVariantImage(savedVariant.getVariantId(), variant);
        }
    }

    private void attachProductImage(Long productId, Product product) {
        Long attachFileId = saveAttachFile(product.getImageObjectKey(), product.getImageMimeType(), product.getImageSize());
        ProductImageEntity image = productImageRepository.findByProductId(productId)
                .orElseGet(() -> {
                    ProductImageEntity created = new ProductImageEntity();
                    created.setProductId(productId);
                    return created;
                });
        image.setAttachFileId(attachFileId);
        productImageRepository.save(image);
    }

    private void attachVariantImage(Long variantId, ProductVariant variant) {
        Long attachFileId = saveAttachFile(variant.getImageObjectKey(), variant.getImageMimeType(), variant.getImageSize());
        ProductVariantImageEntity image = variantImageRepository.findByVariantId(variantId)
                .orElseGet(() -> {
                    ProductVariantImageEntity created = new ProductVariantImageEntity();
                    created.setVariantId(variantId);
                    return created;
                });
        image.setAttachFileId(attachFileId);
        variantImageRepository.save(image);
    }

    private Long saveAttachFile(String objectKey, String mimeType, Integer size) {
        AttachFileEntity attachFile = new AttachFileEntity();
        attachFile.setName(objectKey);
        attachFile.setUrl(objectKey);
        attachFile.setMimeType(mimeType);
        attachFile.setSize(size);
        return attachFileRepository.save(attachFile).getAttachFilesId();
    }

    @Override
    public Optional<ProductVariant> findVariantById(Long variantId) {
        return variantRepository.findById(variantId).map(this::toDomain);
    }

    @Override
    public boolean existsVariantWithSku(String sku, Long excludeProductId) {
        return excludeProductId == null
                ? variantRepository.existsBySku(sku)
                : variantRepository.existsBySkuAndProductIdNot(sku, excludeProductId);
    }

    @Override
    public boolean existsVariantWithBarcode(String barcode, Long excludeProductId) {
        return excludeProductId == null
                ? variantRepository.existsByBarcode(barcode)
                : variantRepository.existsByBarcodeAndProductIdNot(barcode, excludeProductId);
    }

    private Product toDomain(ProductEntity entity) {
        // Mutable on purpose (not .toList()) — ProductUseCase#update appends
        // newly-upserted variants directly onto this list.
        List<ProductVariant> variants = variantRepository.findByProductId(entity.getProductId()).stream()
                .map(this::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));

        String imageObjectKey = productImageRepository.findByProductId(entity.getProductId())
                .flatMap(image -> attachFileRepository.findById(image.getAttachFileId()))
                .map(AttachFileEntity::getUrl)
                .orElse(null);

        return Product.builder()
                .productId(entity.getProductId())
                .name(entity.getName())
                .description(entity.getDescription())
                .categoryId(entity.getCategoryId())
                .brandId(entity.getBrandId())
                .imageObjectKey(imageObjectKey)
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .variants(variants)
                .build();
    }

    private ProductVariant toDomain(ProductVariantEntity entity) {
        List<VariantAttribute> attributes = attributeRepository.findByVariantId(entity.getVariantId()).stream()
                .map(a -> VariantAttribute.builder()
                        .variantAttributeId(a.getVariantAttributeId())
                        .attributeName(a.getAttributeName())
                        .attributeValue(a.getAttributeValue())
                        .build())
                .toList();

        List<ProductUnit> units = unitRepository.findByVariantId(entity.getVariantId()).stream()
                .map(u -> ProductUnit.builder()
                        .productUnitId(u.getProductUnitId())
                        .variantId(u.getVariantId())
                        .unitName(u.getUnitName())
                        .conversionFactor(u.getConversionFactor())
                        .unitPrice(u.getUnitPrice())
                        .barcode(u.getBarcode())
                        .base(u.isBase())
                        .active(u.isActive())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();

        String imageObjectKey = variantImageRepository.findByVariantId(entity.getVariantId())
                .flatMap(image -> attachFileRepository.findById(image.getAttachFileId()))
                .map(AttachFileEntity::getUrl)
                .orElse(null);

        return ProductVariant.builder()
                .variantId(entity.getVariantId())
                .sku(entity.getSku())
                .barcode(entity.getBarcode())
                .price(entity.getPrice())
                .costPrice(entity.getCostPrice())
                .active(entity.isActive())
                .imageObjectKey(imageObjectKey)
                .attributes(attributes)
                .units(units)
                .build();
    }
}
