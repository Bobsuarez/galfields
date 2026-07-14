package co.com.galfields.pos.util;

/**
 * Result of {@link ImageCompressor#compress}: the encoded bytes plus the
 * content type/extension they were actually encoded as, which may differ
 * from the original upload (e.g. a JPEG converted to WebP) and must be used
 * for the stored object key and {@code attach_files.mime_type} instead of
 * the original file's metadata.
 */
public record CompressedImage(byte[] data, String contentType, String extension) {
}
