package hash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PHashDctTest {

    private PHashDct pHashDct;

    @BeforeEach
    public void setUp() {
        pHashDct = new PHashDct();
    }

    /**
     * Creates a solid color image.
     * @param width The width of the image.
     * @param height The height of the image.
     * @param color The color of the image (e.g., 0x000000 for black, 0xFFFFFF for white).
     * @return A BufferedImage with a solid color.
     */
    private BufferedImage createSolidColorImage(int width, int height, int color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, color);
            }
        }
        return img;
    }

    @Test
    public void testName() {
        assertEquals("pHash", pHashDct.name());
    }

    /**
     * Tests the hashing of a completely black image.
     * The DCT of a zero-matrix is a zero-matrix.
     * The mean of the AC coefficients will be 0.
     * No DCT coefficient will be greater than the mean.
     * Therefore, the resulting hash should be 0.
     */
    @Test
    public void testHash_withBlackImage_shouldReturnZero() {
        BufferedImage blackImage = createSolidColorImage(32, 32, 0x000000);
        long expectedHash = 0L;
        long actualHash = pHashDct.hash(blackImage);
        assertEquals(expectedHash, actualHash);
    }

    /**
     * Tests the hashing of a completely white image.
     * For a constant-value matrix, only the DC coefficient (at [0][0]) of the DCT will be non-zero.
     * The mean will be calculated over the AC coefficients (all zero), resulting in a mean of 0.
     * Only the first coefficient (the DC term) is greater than the mean.
     * Therefore, only the first bit of the hash should be set.
     */
    @Test
    public void testHash_withWhiteImage_shouldReturnOne() {
        BufferedImage whiteImage = createSolidColorImage(32, 32, 0xFFFFFF);
        long expectedHash = 1L;
        long actualHash = pHashDct.hash(whiteImage);
        assertEquals(expectedHash, actualHash);
    }

    /**
     * Tests that two different images (black and white) produce different hashes.
     * This is a basic sanity check for the hash function.
     */
    @Test
    public void testHash_withDifferentImages_shouldReturnDifferentHashes() {
        BufferedImage blackImage = createSolidColorImage(32, 32, 0x000000);
        BufferedImage whiteImage = createSolidColorImage(32, 32, 0xFFFFFF);

        long hash1 = pHashDct.hash(blackImage);
        long hash2 = pHashDct.hash(whiteImage);

        assertNotEquals(hash1, hash2);
    }

    /**
     * Tests hashing with a gray image.
     * This ensures the logic works for non-extreme values as well.
     */
    @Test
    public void testHash_withGrayImage() {
        BufferedImage grayImage = createSolidColorImage(32, 32, 0x808080);
        // The hash for a solid gray image should be the same as for a white one (1L)
        // because only the DC coefficient will be non-zero and greater than the mean of the (zero) AC coefficients.
        long expectedHash = 1L;
        long actualHash = pHashDct.hash(grayImage);
        assertEquals(expectedHash, actualHash);
    }
}