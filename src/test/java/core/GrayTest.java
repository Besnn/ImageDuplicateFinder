package core;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class GrayTest {

    /**
     * Tests the Gray.toGray method to ensure the conversion produces a grayscale image.
     * The original image is a 2x2 image with distinct colors.
     */
    @Test
    void testToGrayConversion() {
        // Original 2x2 image with distinct colors
        BufferedImage src = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        src.setRGB(0, 0, Color.RED.getRGB());
        src.setRGB(0, 1, Color.GREEN.getRGB());
        src.setRGB(1, 0, Color.BLUE.getRGB());
        src.setRGB(1, 1, Color.WHITE.getRGB());

        // Convert to grayscale
        BufferedImage grayImage = Gray.toGray(src);

        assertEquals(BufferedImage.TYPE_BYTE_GRAY, grayImage.getType(), "Converted image must be type BYTE_GRAY.");
        assertEquals(2, grayImage.getWidth(), "Width of the converted image must match the source.");
        assertEquals(2, grayImage.getHeight(), "Height of the converted image must match the source.");

        // Ensure pixel intensities are grayscale
        int grayPixel1 = grayImage.getRGB(0, 0) & 0xFF;
        int grayPixel2 = grayImage.getRGB(0, 1) & 0xFF;
        int grayPixel3 = grayImage.getRGB(1, 0) & 0xFF;
        int grayPixel4 = grayImage.getRGB(1, 1) & 0xFF;

        // Verify each pixel value falls within grayscale bounds (0-255) and are valid
        assertTrue(grayPixel1 >= 0 && grayPixel1 <= 255, "Pixel (0,0) intensity must be within grayscale bounds.");
        assertTrue(grayPixel2 >= 0 && grayPixel2 <= 255, "Pixel (0,1) intensity must be within grayscale bounds.");
        assertTrue(grayPixel3 >= 0 && grayPixel3 <= 255, "Pixel (1,0) intensity must be within grayscale bounds.");
        assertEquals(255, grayPixel4, "Pixel (1,1) should remain white in grayscale.");
    }

    /**
     * Tests the Gray.toGray method with a single-color image, ensuring uniform grayscale.
     */
    @Test
    void testToGrayWithUniformColorImage() {
        // Create a 3x3 image filled with blue color
        BufferedImage src = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        int blueRGB = Color.BLUE.getRGB();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                src.setRGB(x, y, blueRGB);
            }
        }

        // Convert to grayscale
        BufferedImage grayImage = Gray.toGray(src);

        assertEquals(BufferedImage.TYPE_BYTE_GRAY, grayImage.getType(), "Converted image must be type BYTE_GRAY.");
        assertEquals(3, grayImage.getWidth(), "Width of the converted image must match the source.");
        assertEquals(3, grayImage.getHeight(), "Height of the converted image must match the source.");

        // Verify all pixels have the same grayscale intensity
        int sampleGrayValue = grayImage.getRGB(0, 0) & 0xFF;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                int grayValue = grayImage.getRGB(x, y) & 0xFF;
                assertEquals(sampleGrayValue, grayValue, "All pixels should have the same grayscale intensity.");
            }
        }
    }
}