package hash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the AHash class.
 */
class AHashTest {

    private AHash aHash;

    @BeforeEach
    void setUp() {
        aHash = new AHash();
    }

    @Test
    @DisplayName("The name() method should return 'aHash'")
    void name() {
        assertEquals("aHash", aHash.name());
    }

    @Test
    @DisplayName("Hashing a completely black image should result in all bits set to 1")
    void hash_blackImage_shouldReturnAllOnes() {
        // Create an 8x8 black image
        BufferedImage blackImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                blackImage.setRGB(x, y, 0x000000); // Black
            }
        }

        // For a black image, all pixel values are 0. The mean is 0.
        // The condition `px[i] >= mean` (0 >= 0) is always true.
        // Therefore, all 64 bits of the hash should be 1.
        long expectedHash = -1L; // 0xFFFFFFFFFFFFFFFFL
        long actualHash = aHash.hash(blackImage);

        assertEquals(expectedHash, actualHash);
    }

    @Test
    @DisplayName("Hashing a completely white image should result in all bits set to 1")
    void hash_whiteImage_shouldReturnAllOnes() {
        // Create an 8x8 white image
        BufferedImage whiteImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                whiteImage.setRGB(x, y, 0xFFFFFF); // White
            }
        }

        // For a white image, all pixel values are 255. The mean is 255.
        // The condition `px[i] >= mean` (255 >= 255) is always true.
        // Therefore, all 64 bits of the hash should be 1.
        long expectedHash = -1L; // 0xFFFFFFFFFFFFFFFFL
        long actualHash = aHash.hash(whiteImage);

        assertEquals(expectedHash, actualHash);
    }

    @Test
    @DisplayName("Hashing an image with a checkerboard pattern should produce the correct hash")
    void hash_checkerboardImage_shouldReturnCorrectPattern() {
        // Create an 8x8 checkerboard image
        BufferedImage checkerboardImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if ((x + y) % 2 == 0) {
                    checkerboardImage.setRGB(x, y, 0xFFFFFF); // White (gray=255)
                } else {
                    checkerboardImage.setRGB(x, y, 0x000000); // Black (gray=0)
                }
            }
        }

        // 32 pixels are white (255) and 32 are black (0).
        // The average gray value (mean) is (32 * 255 + 32 * 0) / 64 = 127.5.
        // White pixels (255) are >= 127.5, so their bits are 1.
        // Black pixels (0) are < 127.5, so their bits are 0.
        // The bit is set if (x+y) % 2 == 0.
        long expectedHash = 0L;
        for (int i = 0; i < 64; i++) {
            int x = i % 8;
            int y = i / 8;
            if ((x + y) % 2 == 0) {
                expectedHash |= (1L << i);
            }
        }

        long actualHash = aHash.hash(checkerboardImage);
        assertEquals(expectedHash, actualHash);
    }
}