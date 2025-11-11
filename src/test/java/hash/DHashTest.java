package hash;

import core.Gray;
import core.Resize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

class DHashTest {

    private DHash dHash;

    @BeforeEach
    void setUp() {
        dHash = new DHash();
    }

    @Test
    @DisplayName("name() should return 'dHash'")
    void testName() {
        assertEquals("dHash", dHash.name());
    }

    @Test
    @DisplayName("Hash of a solid black image should be 0")
    void testHashWithBlackImage() {
        BufferedImage blackImage = createSolidColorImage(9, 8, 0, 0, 0);
        long expectedHash = 0L;

        try (MockedStatic<Gray> mockedGray = Mockito.mockStatic(Gray.class);
             MockedStatic<Resize> mockedResize = Mockito.mockStatic(Resize.class)) {

            mockedGray.when(() -> Gray.toGray(any(BufferedImage.class))).thenReturn(blackImage);
            mockedResize.when(() -> Resize.resize(any(BufferedImage.class), anyInt(), anyInt())).thenReturn(blackImage);

            long actualHash = dHash.hash(blackImage);
            assertEquals(expectedHash, actualHash);
        }
    }

    @Test
    @DisplayName("Hash of a solid white image should be 0")
    void testHashWithWhiteImage() {
        BufferedImage whiteImage = createSolidColorImage(9, 8, 255, 255, 255);
        long expectedHash = 0L;

        try (MockedStatic<Gray> mockedGray = Mockito.mockStatic(Gray.class);
             MockedStatic<Resize> mockedResize = Mockito.mockStatic(Resize.class)) {

            mockedGray.when(() -> Gray.toGray(any(BufferedImage.class))).thenReturn(whiteImage);
            mockedResize.when(() -> Resize.resize(any(BufferedImage.class), anyInt(), anyInt())).thenReturn(whiteImage);

            long actualHash = dHash.hash(whiteImage);
            assertEquals(expectedHash, actualHash);
        }
    }

    @Test
    @DisplayName("Hash of a left-to-right dark-to-light gradient should have all bits set")
    void testHashWithGradientImage() {
        BufferedImage gradientImage = createGradientImage(9, 8);
        long expectedHash = -1L; // This is 0xFFFFFFFFFFFFFFFFL, all bits set to 1

        try (MockedStatic<Gray> mockedGray = Mockito.mockStatic(Gray.class);
             MockedStatic<Resize> mockedResize = Mockito.mockStatic(Resize.class)) {

            mockedGray.when(() -> Gray.toGray(any(BufferedImage.class))).thenReturn(gradientImage);
            mockedResize.when(() -> Resize.resize(any(BufferedImage.class), anyInt(), anyInt())).thenReturn(gradientImage);

            long actualHash = dHash.hash(gradientImage);
            assertEquals(expectedHash, actualHash);
        }
    }

    /**
     * Creates a BufferedImage of a solid color.
     *
     * @param width  Image width.
     * @param height Image height.
     * @param r      Red component (0-255).
     * @param g      Green component (0-255).
     * @param b      Blue component (0-255).
     * @return A new BufferedImage.
     */
    private BufferedImage createSolidColorImage(int width, int height, int r, int g, int b) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int rgb = (r << 16) | (g << 8) | b;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    /**
     * Creates a 9x8 grayscale image with a horizontal gradient.
     * The leftmost pixel is black (0) and the rightmost is lighter.
     * This ensures that `pixel[x] > pixel[x+1]` is always true.
     */
    private BufferedImage createGradientImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grayValue = 255 - (x * 20); // Decreasing value from left to right
                int rgb = (grayValue << 16) | (grayValue << 8) | grayValue;
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }
}