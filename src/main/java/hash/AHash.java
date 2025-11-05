package hash;
import core.Gray;
import core.Resize;
import java.awt.image.BufferedImage;

public class AHash implements Hasher {
    public long hash(BufferedImage img) {
        // Preprocess: convert to grayscale and resize
        img = Gray.toGray(img);
        img = Resize.resize(img, 8, 8);
        
        int w = 8, h = 8;
        double sum = 0;
        int[] px = new int[w*h];
        int idx = 0;
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int rgb = img.getRGB(x, y);
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
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
