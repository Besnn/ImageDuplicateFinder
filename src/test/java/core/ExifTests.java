package core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

public class ExifTests {

    @Test
    void testNoOrientationReturnsOne() {
        assertEquals(1, Exif.readOrientationSafe(Path.of("fixtures/exif-tests/no-orientation.jpg")));
    }

    @Test
    void testNullImageTransformReturnsNull() {
        assertNull(Exif.transform(null, 1));
    }

    @Test
    void testNonExistentFileReturnsOne() {
        assertEquals(1, Exif.readOrientationSafe(Path.of("non/existent/file.jpg")));
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

    @Test
    void testAlphaIsPreserved() {
        BufferedImage src = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, 0x80000000); // 50% transparent black

        // All transformations should preserve the alpha channel type
        for (int i = 1; i <= 8; i++) {
            BufferedImage transformed = Exif.transform(src, i);
            assertNotNull(transformed, "Transform for orientation " + i + " should not be null");
            assertTrue(transformed.getColorModel().hasAlpha(), "Orientation " + i + " should preserve alpha");
            if (i == 1) {
                assertEquals(0x80000000, transformed.getRGB(0, 0), "Orientation 1 should not change pixel value");
            }
        }
    }

    @Test
    void testCorruptedExifReturnsOne(@TempDir Path tempDir) throws IOException {
        Path corruptedFile = tempDir.resolve("corrupted.jpg");
        Files.write(corruptedFile, "not a real jpeg".getBytes());
        assertEquals(1, Exif.readOrientationSafe(corruptedFile));
    }

    private BufferedImage createTestImage() {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.RED.getRGB());
        img.setRGB(1, 0, Color.GREEN.getRGB());
        img.setRGB(0, 1, Color.BLUE.getRGB());
        img.setRGB(1, 1, Color.WHITE.getRGB());
        return img;
    }

    private String colorToString(int rgb) {
        Color c = new Color(rgb); // no alpha
        return String.format("RGB(%d,%d,%d) [0x%08X]",
                c.getRed(), c.getGreen(), c.getBlue(), rgb);
    }

    private void assertPixel(BufferedImage img, int x, int y, Color expected) {
        int actual = img.getRGB(x, y);
        int expectedRgb = expected.getRGB();

        if (actual != expectedRgb) {
            String actualColor = "";
            if (actual == Color.RED.getRGB()) actualColor = "RED";
            else if (actual == Color.GREEN.getRGB()) actualColor = "GREEN";
            else if (actual == Color.BLUE.getRGB()) actualColor = "BLUE";
            else if (actual == Color.WHITE.getRGB()) actualColor = "WHITE";

            String expectedColor = "";
            if (expectedRgb == Color.RED.getRGB()) expectedColor = "RED";
            else if (expectedRgb == Color.GREEN.getRGB()) expectedColor = "GREEN";
            else if (expectedRgb == Color.BLUE.getRGB()) expectedColor = "BLUE";
            else if (expectedRgb == Color.WHITE.getRGB()) expectedColor = "WHITE";

            fail(String.format("Pixel at (%d,%d) mismatch:\n  Expected: %s %s\n  Actual:   %s %s",
                    x, y, expectedColor, colorToString(expectedRgb), actualColor, colorToString(actual)));
        }
    }

    @Test
    void testOrientation2_FlipHorizontal() {
        BufferedImage src = createTestImage();
        BufferedImage result = Exif.transform(src, 2);
        assertEquals(2, result.getWidth());
        assertEquals(2, result.getHeight());
        assertPixel(result, 0, 0, Color.GREEN);  // expects GREEN
        assertPixel(result, 1, 0, Color.RED);    // expects RED
        assertPixel(result, 0, 1, Color.WHITE);  // expects WHITE
        assertPixel(result, 1, 1, Color.BLUE);   // expects BLUE
    }

    @Test
    void testOrientation3_Rotate180() {
        BufferedImage src = createTestImage();
        BufferedImage result = Exif.transform(src, 3);
        assertEquals(2, result.getWidth());
        assertEquals(2, result.getHeight());
        assertPixel(result, 0, 0, Color.WHITE);
        assertPixel(result, 1, 0, Color.BLUE);
        assertPixel(result, 0, 1, Color.GREEN);
        assertPixel(result, 1, 1, Color.RED);
    }

    @Test
    void testOrientation4_FlipVertical() {
        BufferedImage src = createTestImage();
        BufferedImage result = Exif.transform(src, 4);
        assertEquals(2, result.getWidth());
        assertEquals(2, result.getHeight());
        assertPixel(result, 0, 0, Color.BLUE);
        assertPixel(result, 1, 0, Color.WHITE);
        assertPixel(result, 0, 1, Color.RED);
        assertPixel(result, 1, 1, Color.GREEN);
    }

    @Test
    void testOrientation5_Transpose() {
        BufferedImage src = createTestImage();
        BufferedImage result = Exif.transform(src, 5);
        assertEquals(2, result.getWidth());
        assertEquals(2, result.getHeight());
        assertPixel(result, 0, 0, Color.RED);
        assertPixel(result, 1, 0, Color.BLUE);
        assertPixel(result, 0, 1, Color.GREEN);
        assertPixel(result, 1, 1, Color.WHITE);
    }

    @Test
    void testOrientation6_Rotate90() {
        BufferedImage src = createTestImage();
        BufferedImage result = Exif.transform(src, 6);
        assertEquals(2, result.getWidth());
        assertEquals(2, result.getHeight());
        assertPixel(result, 0, 0, Color.BLUE);
        assertPixel(result, 1, 0, Color.RED);
        assertPixel(result, 0, 1, Color.WHITE);
        assertPixel(result, 1, 1, Color.GREEN);
    }

    @Test
    void testOrientation7_Transverse() {
        BufferedImage src = createTestImage();
        BufferedImage result = Exif.transform(src, 7);
        assertEquals(2, result.getWidth());
        assertEquals(2, result.getHeight());
        assertPixel(result, 0, 0, Color.WHITE);
        assertPixel(result, 1, 0, Color.GREEN);
        assertPixel(result, 0, 1, Color.BLUE);
        assertPixel(result, 1, 1, Color.RED);
    }

    @Test
    void testOrientation8_Rotate270() {
        BufferedImage src = createTestImage();
        BufferedImage result = Exif.transform(src, 8);
        assertEquals(2, result.getWidth());
        assertEquals(2, result.getHeight());
        assertPixel(result, 0, 0, Color.GREEN);
        assertPixel(result, 1, 0, Color.WHITE);
        assertPixel(result, 0, 1, Color.RED);
        assertPixel(result, 1, 1, Color.BLUE);
    }
}
