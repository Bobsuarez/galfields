package co.com.galfields.pos.service;

import co.com.galfields.pos.dto.ProductRequest;
import co.com.galfields.pos.dto.ProductResponse;
import co.com.galfields.pos.dto.ProductVariantRequest;
import co.com.galfields.pos.dto.ProductVariantResponse;
import co.com.galfields.pos.dto.VariantAttributeRequest;
import co.com.galfields.pos.dto.VariantAttributeResponse;
import co.com.galfields.pos.entity.AttachFile;
import co.com.galfields.pos.entity.Brand;
import co.com.galfields.pos.entity.Category;
import co.com.galfields.pos.entity.Inventory;
import co.com.galfields.pos.entity.Location;
import co.com.galfields.pos.entity.Product;
import co.com.galfields.pos.entity.ProductImage;
import co.com.galfields.pos.entity.ProductVariant;
import co.com.galfields.pos.entity.ProductVariantImage;
import co.com.galfields.pos.entity.VariantAttribute;
import co.com.galfields.pos.exception.DuplicateResourceException;
import co.com.galfields.pos.exception.ResourceNotFoundException;
import co.com.galfields.pos.repository.AttachFileRepository;
import co.com.galfields.pos.repository.BrandRepository;
import co.com.galfields.pos.repository.CategoryRepository;
import co.com.galfields.pos.repository.InventoryRepository;
import co.com.galfields.pos.repository.LocationRepository;
import co.com.galfields.pos.repository.ProductRepository;
import co.com.galfields.pos.repository.ProductVariantRepository;
import co.com.galfields.pos.util.ImageCompressor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String DEFAULT_LOCATION_NAME = "Bodega Principal";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final AttachFileRepository attachFileRepository;
    private final MinioService minioService;
    private final ImageCompressor imageCompressor;

    @Transactional
    public ProductResponse createProduct(
            ProductRequest request,
            MultipartFile image,
            List<ProductVariantRequest> variantRequests,
            Map<Integer, MultipartFile> variantImages
    ) {

        validateNoDuplicateSkuOrBarcode(variantRequests, null);

        Product product = new Product();
        applyProductFields(product, request);
        product = productRepository.save(product);

        for (int i = 0; i < variantRequests.size(); i++) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            product.getVariants()
                    .add(variant);
            applyVariantFields(variant, variantRequests.get(i));
            productVariantRepository.save(variant);

            applyVariantAttributes(
                    variant, variantRequests.get(i)
                            .attributes()
            );

            Inventory inventory = new Inventory();
            inventory.setVariant(variant);
            inventory.setLocation(defaultLocation());
            inventory.setQuantityOnHand(Optional.ofNullable(variantRequests.get(i)
                                                                    .initialStock())
                                                .orElse(0));
            inventoryRepository.save(inventory);

            MultipartFile variantImage = variantImages.get(i);
            if (variantImage != null && !variantImage.isEmpty()) {
                attachVariantImage(product, variant, variantImage);
            }
        }

        if (image != null && !image.isEmpty()) {
            attachProductImage(product, image);
        }

        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        return toResponse(findProductOrThrow(productId));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(Pageable pageable) {
        
        return productRepository.findByActiveTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ProductResponse updateProduct(
            Long productId, ProductRequest request, MultipartFile image,
            List<ProductVariantRequest> variantRequests, Map<Integer, MultipartFile> variantImages
    ) {
        Product product = findProductOrThrow(productId);
        applyProductFields(product, request);

        if (variantRequests != null && !variantRequests.isEmpty()) {
            validateNoDuplicateSkuOrBarcode(variantRequests, productId);

            for (int i = 0; i < variantRequests.size(); i++) {
                ProductVariantRequest variantRequest = variantRequests.get(i);
                ProductVariant variant = findVariantBySku(product, variantRequest.sku()).orElse(null);
                if (variant == null) {
                    variant = new ProductVariant();
                    variant.setProduct(product);
                    product.getVariants()
                            .add(variant);
                }

                applyVariantFields(variant, variantRequest);
                productVariantRepository.save(variant);
                applyVariantAttributes(variant, variantRequest.attributes());

                if (variantRequest.initialStock() != null) {
                    upsertInventory(variant, variantRequest.initialStock());
                }

                MultipartFile variantImage = variantImages.get(i);
                if (variantImage != null && !variantImage.isEmpty()) {
                    attachVariantImage(product, variant, variantImage);
                }
            }
        }

        if (image != null && !image.isEmpty()) {
            attachProductImage(product, image);
        }

        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProductOrThrow(productId);
        product.setActive(false);
        product.getVariants()
                .forEach(v -> v.setActive(false));
        productRepository.save(product);
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
    }

    private Optional<ProductVariant> findVariantBySku(Product product, String sku) {
        return product.getVariants()
                .stream()
                .filter(v -> v.getSku()
                        .equals(sku))
                .findFirst();
    }

    private Location defaultLocation() {
        return locationRepository.findByName(DEFAULT_LOCATION_NAME)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Default location '" + DEFAULT_LOCATION_NAME + "' not found"));
    }

    private Integer stockOf(ProductVariant variant) {
        Location location = defaultLocation();
        return inventoryRepository
                .findByVariant_VariantIdAndLocation_LocationId(variant.getVariantId(), location.getLocationId())
                .map(Inventory::getQuantityOnHand)
                .orElse(0);
    }

    private void upsertInventory(ProductVariant variant, Integer quantity) {
        Location location = defaultLocation();
        Inventory inventory = inventoryRepository
                .findByVariant_VariantIdAndLocation_LocationId(variant.getVariantId(), location.getLocationId())
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setVariant(variant);
                    created.setLocation(location);
                    return created;
                });
        inventory.setQuantityOnHand(quantity);
        inventoryRepository.save(inventory);
    }

    private void validateNoDuplicateSkuOrBarcode(
            List<ProductVariantRequest> variantRequests,
            Long excludeProductId
    ) {
        Set<String> skusSeen = new HashSet<>();
        Set<String> barcodesSeen = new HashSet<>();

        for (ProductVariantRequest variantRequest : variantRequests) {
            if (!skusSeen.add(variantRequest.sku())) {
                throw new DuplicateResourceException("Duplicate sku '" + variantRequest.sku() + "' in request");
            }
            if (!barcodesSeen.add(variantRequest.barcode())) {
                throw new DuplicateResourceException("Duplicate barcode '" + variantRequest.barcode() + "' in request");
            }

            boolean skuTaken = excludeProductId == null
                    ? productVariantRepository.existsBySku(variantRequest.sku())
                    : productVariantRepository.existsBySkuAndProduct_ProductIdNot(
                    variantRequest.sku(), excludeProductId);
            if (skuTaken) {
                throw new DuplicateResourceException(
                        "A product variant with sku '" + variantRequest.sku() + "' already exists");
            }

            boolean barcodeTaken = excludeProductId == null
                    ? productVariantRepository.existsByBarcode(variantRequest.barcode())
                    : productVariantRepository.existsByBarcodeAndProduct_ProductIdNot(
                    variantRequest.barcode(), excludeProductId);
            if (barcodeTaken) {
                throw new DuplicateResourceException(
                        "A product variant with barcode '" + variantRequest.barcode() + "' already exists");
            }
        }
    }

    private void applyProductFields(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category " + request.categoryId() + " not found"));
        product.setCategory(category);

        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand " + request.brandId() + " not found"));
        product.setBrand(brand);
    }

    private void applyVariantFields(ProductVariant variant, ProductVariantRequest variantRequest) {
        variant.setSku(variantRequest.sku());
        variant.setBarcode(variantRequest.barcode());
        variant.setPrice(variantRequest.price());
        variant.setCostPrice(variantRequest.costPrice());
    }

    private void applyVariantAttributes(ProductVariant variant, List<VariantAttributeRequest> attributeRequests) {
        if (attributeRequests == null) {
            return;
        }

        // Update matching (variant_id, attribute_name) rows in place and only
        // add/remove what actually changed, instead of clear()-then-re-add: with
        // orphanRemoval, Hibernate can flush the re-inserts before the deletes of
        // the cleared rows, tripping the unique (variant_id, attribute_name)
        // constraint on names that are unchanged across the update.
        Map<String, VariantAttribute> existingByName = variant.getAttributes()
                .stream()
                .collect(Collectors.toMap(VariantAttribute::getAttributeName, a -> a));
        Set<String> requestedNames = attributeRequests.stream()
                .map(VariantAttributeRequest::name)
                .collect(Collectors.toSet());

        variant.getAttributes()
                .removeIf(a -> !requestedNames.contains(a.getAttributeName()));

        for (VariantAttributeRequest attributeRequest : attributeRequests) {
            VariantAttribute attribute = existingByName.get(attributeRequest.name());
            if (attribute != null) {
                attribute.setAttributeValue(attributeRequest.value());
            } else {
                VariantAttribute created = new VariantAttribute();
                created.setVariant(variant);
                created.setAttributeName(attributeRequest.name());
                created.setAttributeValue(attributeRequest.value());
                variant.getAttributes()
                        .add(created);
            }
        }
    }

    private void attachProductImage(Product product, MultipartFile file) {
        byte[] compressed = imageCompressor.compress(file);
        String objectKey = minioService.uploadProductImage(product, file, compressed);
        AttachFile attachFile = saveAttachFile(file, compressed, objectKey);

        ProductImage productImage = product.getImage();
        String previousObjectKey = null;
        if (productImage == null) {
            productImage = new ProductImage();
            productImage.setProduct(product);
            product.setImage(productImage);
        } else {
            previousObjectKey = productImage.getAttachFile()
                    .getUrl();
        }
        productImage.setAttachFile(attachFile);

        if (previousObjectKey != null) {
            minioService.deleteObject(previousObjectKey);
        }
    }

    private void attachVariantImage(Product product, ProductVariant variant, MultipartFile file) {
        byte[] compressed = imageCompressor.compress(file);
        String objectKey = minioService.uploadVariantImage(product, variant, file, compressed);
        AttachFile attachFile = saveAttachFile(file, compressed, objectKey);

        ProductVariantImage variantImage = variant.getImage();
        String previousObjectKey = null;
        if (variantImage == null) {
            variantImage = new ProductVariantImage();
            variantImage.setVariant(variant);
            variant.setImage(variantImage);
        } else {
            previousObjectKey = variantImage.getAttachFile()
                    .getUrl();
        }
        variantImage.setAttachFile(attachFile);

        if (previousObjectKey != null) {
            minioService.deleteObject(previousObjectKey);
        }
    }

    private AttachFile saveAttachFile(MultipartFile file, byte[] data, String objectKey) {
        AttachFile attachFile = new AttachFile();
        attachFile.setName(file.getOriginalFilename() != null ? file.getOriginalFilename() : objectKey);
        attachFile.setUrl(objectKey);
        attachFile.setMimeType(file.getContentType());
        attachFile.setSize(data.length);
        return attachFileRepository.save(attachFile);
    }

    private ProductResponse toResponse(Product product) {
        List<ProductVariantResponse> variantResponses = product.getVariants()
                .stream()
                .map(this::toVariantResponse)
                .toList();

        String imageUrl = Optional.ofNullable(product.getImage())
                .map(ProductImage::getAttachFile)
                .map(AttachFile::getUrl)
                .map(minioService::getPublicUrl)
                .orElse(null);

        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                Optional.ofNullable(product.getCategory())
                        .map(Category::getCategoryId)
                        .orElse(null),
                Optional.ofNullable(product.getCategory())
                        .map(Category::getName)
                        .orElse(null),
                Optional.ofNullable(product.getBrand())
                        .map(Brand::getBrandId)
                        .orElse(null),
                Optional.ofNullable(product.getBrand())
                        .map(Brand::getName)
                        .orElse(null),
                imageUrl,
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                variantResponses
        );
    }

    private ProductVariantResponse toVariantResponse(ProductVariant variant) {
        String imageUrl = Optional.ofNullable(variant.getImage())
                .map(ProductVariantImage::getAttachFile)
                .map(AttachFile::getUrl)
                .map(minioService::getPublicUrl)
                .orElse(null);

        List<VariantAttributeResponse> attributes = variant.getAttributes()
                .stream()
                .map(a -> new VariantAttributeResponse(a.getAttributeName(), a.getAttributeValue()))
                .toList();

        return new ProductVariantResponse(
                variant.getVariantId(),
                variant.getSku(),
                variant.getBarcode(),
                variant.getPrice(),
                variant.getCostPrice(),
                stockOf(variant),
                imageUrl,
                variant.isActive(),
                attributes
        );
    }
}
