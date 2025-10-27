package hash;

public final class Hamming {
    public static int distance(long a, long b) {
        return Long.bitCount(a ^ b);
    }
}
