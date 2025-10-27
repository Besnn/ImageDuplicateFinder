package hash;
import java.awt.image.BufferedImage;

public class AHash implements Hasher {
    public long hash(BufferedImage img) {
        int w = 8, h = 8;
        double sum = 0;
        int[] px = new int[w*h];
        int idx = 0;
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int rgb = img.getRGB(x, y);
            int gray = rgb & 0xff;
            sum += gray;
            px[idx++] = gray;
        }
        double mean = sum / (w*h);
        long bits = 0L;
        for (int i=0;i<64;i++)
            if (px[i] >= mean) bits |= (1L << i);
        return bits;
    }
    public String name() { return "aHash"; }
}
