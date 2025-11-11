package hash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@link Hamming} class.
 */
class HammingTest {

    @Test
    void testDistanceWithSameValues() {
        assertEquals(0, Hamming.distance(0L, 0L), "Distance between a number and itself should be 0.");
        assertEquals(0, Hamming.distance(12345L, 12345L), "Distance between a number and itself should be 0.");
        assertEquals(0, Hamming.distance(-1L, -1L), "Distance between a number and itself should be 0.");
        assertEquals(0, Hamming.distance(Long.MAX_VALUE, Long.MAX_VALUE), "Distance between a number and itself should be 0.");
    }

    @Test
    void testDistanceWithDifferentValues() {
        // 10 (00...1010) vs 13 (00...1101) -> XOR is 7 (00...0111), bit count is 3
        assertEquals(3, Hamming.distance(10L, 13L), "Distance between 10 and 13 should be 3.");
    }

    @Test
    void testDistanceIsSymmetric() {
        assertEquals(3, Hamming.distance(13L, 10L), "Distance should be symmetric.");
    }

    @Test
    void testDistanceWithZero() {
        // distance from 0 is just the bit count of the other number
        assertEquals(1, Hamming.distance(0L, 1L));
        assertEquals(1, Hamming.distance(0L, 2L));
        assertEquals(2, Hamming.distance(0L, 3L));
        assertEquals(63, Hamming.distance(0L, Long.MAX_VALUE), "Distance between 0 and Long.MAX_VALUE should be 63.");
    }

    @Test
    void testDistanceWithNegativeNumbers() {
        // -1L in two's complement is all 64 bits set to 1.
        // 0L is all 0s. XOR is all 1s. Bit count is 64.
        assertEquals(64, Hamming.distance(0L, -1L), "Distance between 0 and -1 should be 64.");

        // Long.MIN_VALUE is 1 followed by 63 zeros.
        // Long.MAX_VALUE is 0 followed by 63 ones.
        // XOR is all 64 bits set to 1.
        assertEquals(64, Hamming.distance(Long.MIN_VALUE, Long.MAX_VALUE), "Distance between Long.MIN_VALUE and Long.MAX_VALUE should be 64.");
    }
}