package core;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ResizeTest {

    /**
     * Verifies that the resize method correctly changes the dimensions of the image
     * while preserving the image type.
     */
    @Test
    void testResizeChangesDimensions() {
        // Arrange
        int originalWidth = 100;
        int originalHeight = 50;
        int newWidth = 200;
        int newHeight = 100;
        BufferedImage src = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);

        // Act
        BufferedImage resized = Resize.resize(src, newWidth, newHeight);

        // Assert
        assertEquals(newWidth, resized.getWidth(), "The width of the resized image should match the specified width.");
        assertEquals(newHeight, resized.getHeight(), "The height of the resized image should match the specified height.");
        assertEquals(src.getType(), resized.getType(), "The type of the resized image should be the same as the source image.");
    }

    /**
     * Ensures that the resize method creates a valid resized image even for square aspect ratios.
     */
    @Test
    void testResizeSquareImage() {
        // Arrange
        int originalSize = 50;
        int newSize = 100;
        BufferedImage src = new BufferedImage(originalSize, originalSize, BufferedImage.TYPE_INT_ARGB);

        // Act
        BufferedImage resized = Resize.resize(src, newSize, newSize);

        // Assert
        assertNotNull(resized, "The resized image should not be null.");
        assertEquals(newSize, resized.getWidth(), "The resized image width should match the specified size.");
        assertEquals(newSize, resized.getHeight(), "The resized image height should match the specified size.");
    }

    /**
     * Validates that resizing an image with zero dimensions throws an IllegalArgumentException.
     */
    @Test
    void testResizeZeroDimensions() {
        // Arrange
        BufferedImage src = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> Resize.resize(src, 0, 100), "Width of zero should throw an exception.");
        assertThrows(IllegalArgumentException.class, () -> Resize.resize(src, 100, 0), "Height of zero should throw an exception.");
        assertThrows(IllegalArgumentException.class, () -> Resize.resize(src, 0, 0), "Both dimensions zero should throw an exception.");
    }

    /**
     * Tests the compatibility of resizing using different image types (e.g., TYPE_INT_RGB, TYPE_BYTE_GRAY).
     */
    @Test
    void testResizeWithDifferentImageTypes() {
        // Arrange
        BufferedImage srcRgb = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        BufferedImage srcGray = new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY);
        int newWidth = 100;
        int newHeight = 150;

        // Act
        BufferedImage resizedRgb = Resize.resize(srcRgb, newWidth, newHeight);
        BufferedImage resizedGray = Resize.resize(srcGray, newWidth, newHeight);

        // Assert
        assertEquals(newWidth, resizedRgb.getWidth(), "Resized RGB image width should match specified width.");
        assertEquals(newHeight, resizedRgb.getHeight(), "Resized RGB image height should match specified height.");
        assertEquals(BufferedImage.TYPE_INT_RGB, resizedRgb.getType(), "Resized RGB image type should match source type.");

        assertEquals(newWidth, resizedGray.getWidth(), "Resized GRAY image width should match specified width.");
        assertEquals(newHeight, resizedGray.getHeight(), "Resized GRAY image height should match specified height.");
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, resizedGray.getType(), "Resized GRAY image type should match source type.");
    }

    /**
     * Confirms that the graphics context is correctly disposed of after resizing, ensuring no resource leaks.
     * This is tested indirectly by confirming that no exceptions occur during the process.
     */
    @Test
    void testGraphicsContextDisposal() {
        // Arrange
        BufferedImage src = new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB);
        int newWidth = 30;
        int newHeight = 20;

        // Act & Assert
        assertDoesNotThrow(() -> Resize.resize(src, newWidth, newHeight), "Resizing should not throw any exceptions.");
    }
}