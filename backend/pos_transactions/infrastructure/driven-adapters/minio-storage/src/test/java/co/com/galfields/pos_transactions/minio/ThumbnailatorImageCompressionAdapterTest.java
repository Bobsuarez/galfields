package co.com.galfields.pos_transactions.minio;

import co.com.galfields.pos_transactions.model.catalog.CompressedImage;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

class ThumbnailatorImageCompressionAdapterTest {

    private final ThumbnailatorImageCompressionAdapter adapter = new ThumbnailatorImageCompressionAdapter();

    @Test
    void passesThroughUnrecognizedContentTypeUnchanged() {
        byte[] data = {1, 2, 3};
        CompressedImage result = adapter.compress(data, "image/gif", "anim.gif");

        assertThat(result.data()).isEqualTo(data);
        assertThat(result.contentType()).isEqualTo("image/gif");
        assertThat(result.extension()).isEqualTo(".gif");
    }

    @Test
    void reencodesPngToWebp() throws Exception {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);

        CompressedImage result = adapter.compress(out.toByteArray(), "image/png", "photo.png");

        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.extension()).isEqualTo(".webp");
        assertThat(result.data()).isNotEmpty();
    }
}
