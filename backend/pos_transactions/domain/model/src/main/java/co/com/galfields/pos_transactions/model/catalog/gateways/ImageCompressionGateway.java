package co.com.galfields.pos_transactions.model.catalog.gateways;

import co.com.galfields.pos_transactions.model.catalog.CompressedImage;

public interface ImageCompressionGateway {
    CompressedImage compress(byte[] data, String contentType, String originalFilename);
}
