package co.com.galfields.pos_transactions.model.catalog;

import java.text.Normalizer;
import java.util.Locale;

/** Mirrors backend/pos's MinioService#slugify — used to build the MinIO
 * folder path for an uploaded image (business naming, not storage-tech, so
 * it stays in the domain/usecase layer rather than the ImageStorageGateway
 * adapter). */
public final class Slug {

    private Slug() {
    }

    public static String of(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "sin_nombre" : slug;
    }
}
