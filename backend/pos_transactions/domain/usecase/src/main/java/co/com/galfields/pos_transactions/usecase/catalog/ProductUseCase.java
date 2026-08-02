package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.DuplicateResourceException;
import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.Brand;
import co.com.galfields.pos_transactions.model.catalog.Category;
import co.com.galfields.pos_transactions.model.catalog.CompressedImage;
import co.com.galfields.pos_transactions.model.PageQuery;
import co.com.galfields.pos_transactions.model.PageResult;
import co.com.galfields.pos_transactions.model.catalog.Product;
import co.com.galfields.pos_transactions.model.catalog.ProductUnit;
import co.com.galfields.pos_transactions.model.catalog.ProductVariant;
import co.com.galfields.pos_transactions.model.catalog.Slug;
import co.com.galfields.pos_transactions.model.catalog.gateways.BrandRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.CategoryRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageCompressionGateway;
import co.com.galfields.pos_transactions.model.catalog.gateways.ImageStorageGateway;
import co.com.galfields.pos_transactions.model.catalog.gateways.LocationCatalogRepository;
import co.com.galfields.pos_transactions.model.catalog.gateways.ProductRepository;
import co.com.galfields.pos_transactions.model.inventory.Inventory;
import co.com.galfields.pos_transactions.model.inventory.gateways.InventoryRepository;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Mirrors backend/pos's ProductService. A product and its variants are
 * created/updated together; variants are upserted by sku on update
 * (unmatched sku = new variant, omitted variants are left untouched).
 * Reuses Fase 3's real {@link InventoryRepository} for stock reads/writes
 * (Inventario already exists by the time this fase runs — no need for a
 * private shadow copy the way Fases 2/3 needed for each other).
 */
@RequiredArgsConstructor
public class ProductUseCase {

    private static final String DEFAULT_LOCATION_NAME = "Bogotá - Chapinero";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final LocationCatalogRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final ImageCompressionGateway imageCompressionGateway;
    private final ImageStorageGateway imageStorageGateway;

    public record UploadedImage(byte[] data, String contentType, String originalFilename) {
    }

    public Product create(Product product, UploadedImage productImage, Map<Integer, UploadedImage> variantImages) {
        validateCategoryAndBrand(product);
        validateNoDuplicateSkuOrBarcode(product.getVariants(), null);

        Product saved = productRepository.save(product);

        Long locationId = defaultLocationId();
        // Loop over the original request's variants, not saved.getVariants()
        // — the adapter mutates variantId onto these same objects in place
        // (so the ids are real after save()), but initialStock is a
        // write-only field never persisted/re-read, so saved's freshly
        // re-queried variants would always show it as null here.
        for (ProductVariant variant : product.getVariants()) {
            int initialStock = variant.getInitialStock() != null ? variant.getInitialStock() : 0;
            upsertInventory(variant.getVariantId(), locationId, initialStock);
        }

        if (productImage != null || !variantImages.isEmpty()) {
            attachImages(saved, productImage, variantImages);
            saved = productRepository.save(saved);
        }

        return enrich(saved);
    }

    public Product get(Long productId) {
        return enrich(findOrThrow(productId));
    }

    public PageResult<Product> list(PageQuery query, boolean includeInactive) {
        PageResult<Product> page = productRepository.findPage(query, includeInactive);
        page.content().forEach(this::enrich);
        return page;
    }

    public Product update(Long productId, Product patch, UploadedImage productImage,
                           List<ProductVariant> variantPatches, Map<Integer, UploadedImage> variantImages) {
        Product existing = findOrThrow(productId);
        existing.setName(patch.getName());
        existing.setDescription(patch.getDescription());
        existing.setCategoryId(patch.getCategoryId());
        existing.setBrandId(patch.getBrandId());
        validateCategoryAndBrand(existing);

        if (variantPatches != null && !variantPatches.isEmpty()) {
            validateNoDuplicateSkuOrBarcode(variantPatches, productId);

            for (ProductVariant variantPatch : variantPatches) {
                ProductVariant target = findVariantBySku(existing, variantPatch.getSku()).orElse(null);
                if (target == null) {
                    target = ProductVariant.builder().active(true).build();
                    existing.getVariants().add(target);
                }
                target.setSku(variantPatch.getSku());
                target.setBarcode(variantPatch.getBarcode());
                target.setPrice(variantPatch.getPrice());
                target.setCostPrice(variantPatch.getCostPrice());
                if (variantPatch.getAttributes() != null) {
                    target.setAttributes(variantPatch.getAttributes());
                }
            }
        }

        Product saved = productRepository.save(existing);

        if (variantPatches != null) {
            Long locationId = defaultLocationId();
            for (ProductVariant variantPatch : variantPatches) {
                if (variantPatch.getInitialStock() != null) {
                    ProductVariant target = findVariantBySku(saved, variantPatch.getSku())
                            .orElseThrow(() -> new ResourceNotFoundException("Variant '" + variantPatch.getSku() + "' not found after save"));
                    upsertInventory(target.getVariantId(), locationId, variantPatch.getInitialStock());
                }
            }
        }

        Map<Integer, UploadedImage> images = variantImages == null ? Map.of() : variantImages;
        if (productImage != null || !images.isEmpty()) {
            attachImages(saved, productImage, images);
            saved = productRepository.save(saved);
        }

        return enrich(saved);
    }

    /** Soft-deactivate: flips the product and every one of its variants to
     * inactive, never removes rows. */
    public void deactivate(Long productId) {
        Product product = findOrThrow(productId);
        product.setActive(false);
        product.getVariants().forEach(v -> v.setActive(false));
        productRepository.save(product);
    }

    /** Counterpart to {@link #deactivate} — flips everything back to active. */
    public Product activate(Long productId) {
        Product product = findOrThrow(productId);
        product.setActive(true);
        product.getVariants().forEach(v -> v.setActive(true));
        return enrich(productRepository.save(product));
    }

    private Product findOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
    }

    private Optional<ProductVariant> findVariantBySku(Product product, String sku) {
        return product.getVariants().stream().filter(v -> v.getSku().equals(sku)).findFirst();
    }

    private Long defaultLocationId() {
        return locationRepository.findByName(DEFAULT_LOCATION_NAME)
                .map(l -> l.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Default location '" + DEFAULT_LOCATION_NAME + "' not found"));
    }

    private void upsertInventory(Long variantId, Long locationId, int quantity) {
        Inventory inventory = inventoryRepository.findByVariantAndLocation(variantId, locationId)
                .orElseGet(() -> Inventory.builder().variantId(variantId).locationId(locationId).quantityOnHand(0).build());
        inventory.setQuantityOnHand(quantity);
        inventoryRepository.save(inventory);
    }

    private void validateCategoryAndBrand(Product product) {
        Category category = categoryRepository.findById(product.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category " + product.getCategoryId() + " not found"));
        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand " + product.getBrandId() + " not found"));
        product.setCategoryName(category.getName());
        product.setBrandName(brand.getName());
    }

    private void validateNoDuplicateSkuOrBarcode(List<ProductVariant> variants, Long excludeProductId) {
        Set<String> skusSeen = new HashSet<>();
        Set<String> barcodesSeen = new HashSet<>();

        for (ProductVariant variant : variants) {
            if (!skusSeen.add(variant.getSku())) {
                throw new DuplicateResourceException("Duplicate sku '" + variant.getSku() + "' in request");
            }
            if (!barcodesSeen.add(variant.getBarcode())) {
                throw new DuplicateResourceException("Duplicate barcode '" + variant.getBarcode() + "' in request");
            }
            if (productRepository.existsVariantWithSku(variant.getSku(), excludeProductId)) {
                throw new DuplicateResourceException("A product variant with sku '" + variant.getSku() + "' already exists");
            }
            if (productRepository.existsVariantWithBarcode(variant.getBarcode(), excludeProductId)) {
                throw new DuplicateResourceException("A product variant with barcode '" + variant.getBarcode() + "' already exists");
            }
        }
    }

    private void attachImages(Product product, UploadedImage productImage, Map<Integer, UploadedImage> variantImages) {
        if (productImage != null) {
            String folder = "files/%s/%s".formatted(categorySlug(product), Slug.of(product.getName()));
            UploadedCompressedImage compressed = uploadReplacing(product.getImageObjectKey(), folder, productImage);
            product.setImageObjectKey(compressed.objectKey());
            product.setImageMimeType(compressed.image().contentType());
            product.setImageSize(compressed.image().data().length);
        }

        for (int i = 0; i < product.getVariants().size(); i++) {
            UploadedImage variantImage = variantImages.get(i);
            if (variantImage == null) {
                continue;
            }
            ProductVariant variant = product.getVariants().get(i);
            String folder = "files/%s/%s/variants/%s".formatted(categorySlug(product), Slug.of(product.getName()), Slug.of(variant.getSku()));
            UploadedCompressedImage compressed = uploadReplacing(variant.getImageObjectKey(), folder, variantImage);
            variant.setImageObjectKey(compressed.objectKey());
            variant.setImageMimeType(compressed.image().contentType());
            variant.setImageSize(compressed.image().data().length);
        }
    }

    private record UploadedCompressedImage(String objectKey, CompressedImage image) {
    }

    private UploadedCompressedImage uploadReplacing(String previousKey, String folder, UploadedImage image) {
        CompressedImage compressed =
                imageCompressionGateway.compress(image.data(), image.contentType(), image.originalFilename());
        String newKey = imageStorageGateway.upload(folder, compressed);
        if (previousKey != null) {
            imageStorageGateway.delete(previousKey);
        }
        return new UploadedCompressedImage(newKey, compressed);
    }

    private String categorySlug(Product product) {
        return product.getCategoryName() != null ? Slug.of(product.getCategoryName()) : "sin_categoria";
    }

    private Product enrich(Product product) {
        Long locationId = defaultLocationId();
        product.setImageUrl(imageStorageGateway.getPublicUrl(product.getImageObjectKey()));
        // categoryName/brandName aren't persisted columns (ProductEntity only
        // stores the FK ids) — resolve them here so every read path (get,
        // list, and the object returned by create/update) is consistent,
        // rather than relying on whatever the usecase happened to already
        // have in memory at save time.
        categoryRepository.findById(product.getCategoryId()).ifPresent(c -> product.setCategoryName(c.getName()));
        brandRepository.findById(product.getBrandId()).ifPresent(b -> product.setBrandName(b.getName()));

        for (ProductVariant variant : product.getVariants()) {
            int stock = inventoryRepository.findByVariantAndLocation(variant.getVariantId(), locationId)
                    .map(Inventory::getQuantityOnHand)
                    .orElse(0);
            variant.setStock(stock);
            variant.setImageUrl(imageStorageGateway.getPublicUrl(variant.getImageObjectKey()));

            if (variant.getUnits() != null) {
                for (ProductUnit unit : variant.getUnits()) {
                    unit.setStock(Math.floorDiv(stock, unit.getConversionFactor()));
                }
            }
        }
        return product;
    }
}
