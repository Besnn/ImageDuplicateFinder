package core;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageLoaderTest {

    /**
     * Verifies that the load method correctly loads an image file
     * and applies EXIF orientation for valid inputs.
     */
    @Test
    void testLoadValidImage() throws Exception {
        // Arrange
        Path validImagePath = Path.of("fixtures/exif-tests/no-orientation.jpg");
        assertTrue(Files.exists(validImagePath), "Test file does not exist.");

        // Act
        BufferedImage result = ImageLoader.load(validImagePath);

        // Assert
        assertNotNull(result, "Expected a loaded BufferedImage object.");
        assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
    }

    /**
     * Validates that an exception is thrown when trying to load a non-image file.
     */
    @Test
    void testLoadInvalidFileFormat() {
        // Arrange
        Path invalidFilePath = Path.of("fixtures/exif-tests/some-text-file.txt");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ImageLoader.load(invalidFilePath));
        assertEquals("Unsupported format: " + invalidFilePath, exception.getMessage());
    }

    /**
     * Ensures load method throws an exception when the file path is invalid or non-existent.
     */
    @Test
    void testLoadNonExistentFile() {
        // Arrange
        Path nonExistentPath = Path.of("fixtures/exif-tests/does-not-exist.jpg");

        // Act & Assert
        assertThrows(Exception.class, () -> ImageLoader.load(nonExistentPath));
    }

    /**
     * Verifies that load method applies EXIF transformations correctly.
     * Mocking EXIF-related behavior for isolated testing.
     */
    @Test
    void testLoadAppliesExifOrientation() throws Exception {
        // Arrange
        Path validImagePath = Path.of("fixtures/exif-tests/cat.jpg");
        BufferedImage mockImage = mock(BufferedImage.class);
        assertTrue(Files.exists(validImagePath), "Test file does not exist.");

        // Mock core.Exif.applyOrientation behavior
        mockStatic(core.Exif.class);
        when(core.Exif.applyOrientation(any(BufferedImage.class), eq(validImagePath))).thenReturn(mockImage);

        // Act
        BufferedImage result = ImageLoader.load(validImagePath);

        // Assert
        assertNotNull(result, "Expected a loaded and transformed BufferedImage object.");
        assertSame(mockImage, result, "Expected the EXIF-transformed BufferedImage.");
    }
}