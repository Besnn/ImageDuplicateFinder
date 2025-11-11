// java
package hash;

import core.Gray;
import core.Resize;

import java.awt.image.BufferedImage;

public class PHashDct implements Hasher {

    private static final double EPS = 1e-9;

    public long hash(BufferedImage img) {
        // Preprocess: convert to grayscale and resize
        img = Gray.toGray(img);
        img = Resize.resize(img, 32, 32);

        int N = 32;
        double[][] vals = new double[N][N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                double gray = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
                vals[y][x] = gray;
            }
        }

        double[][] dct = dct(vals);

        // Top-left 8x8 block with numeric stabilization
        double[] low = new double[64];
        int idx = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                double v = dct[y][x];
                if (Math.abs(v) < EPS) v = 0.0; // zero-out tiny noise
                low[idx++] = v;
            }
        }

        // Mean of AC coefficients (skip DC), ignore near-zero values
        double mean = java.util.Arrays.stream(low, 1, 64)
                .filter(v -> Math.abs(v) >= EPS)
                .average()
                .orElse(0.0);

        long bits = 0L;
        for (int i = 0; i < 64; i++) {
            if (low[i] - mean > EPS) {
                bits |= (1L << i);
            }
        }
        return bits;
    }

    private static double[][] dct(double[][] f) {
        int N = f.length;
        double[][] F = new double[N][N];
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int x = 0; x < N; x++) {
                    for (int y = 0; y < N; y++) {
                        sum += f[x][y]
                                * Math.cos(((2 * x + 1) * u * Math.PI) / (2 * N))
                                * Math.cos(((2 * y + 1) * v * Math.PI) / (2 * N));
                    }
                }
                double cu = (u == 0) ? Math.sqrt(1.0 / N) : Math.sqrt(2.0 / N);
                double cv = (v == 0) ? Math.sqrt(1.0 / N) : Math.sqrt(2.0 / N);
                F[u][v] = cu * cv * sum;
            }
        }
        return F;
    }

    public String name() {
        return "pHash";
    }
}
