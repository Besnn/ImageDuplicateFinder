import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import core.Exif;

public class TestTest {

    @Test
    void testNoOrientationReturnsOne() {
        assertEquals(1, Exif.readOrientationSafe(Path.of("nonexistent.jpg")));
    }

    @Test
    void testNullImageTransformReturnsNull() {
        assertNull(Exif.transform(null, 1));
    }

    @Test
    void testInvalidOrientationRetainsOriginal() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        assertSame(img, Exif.transform(img, 0));
        assertSame(img, Exif.transform(img, 9));
    }

    @Test
    void testOrientation1RetainsOriginal() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        assertSame(img, Exif.transform(img, 1));
    }
}
