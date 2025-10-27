package hash;
import java.awt.image.BufferedImage;

public class DHash implements Hasher {
    public long hash(BufferedImage img) {
        int w = 9, h = 8;
        long bits = 0L;
        int i=0;
        for (int y=0;y<h;y++) {
            for (int x=0;x<w-1;x++) {
                int left = img.getRGB(x, y) & 0xff;
                int right = img.getRGB(x+1, y) & 0xff;
                if (left > right) bits |= (1L << i);
                i++;
            }
        }
        return bits;
    }
    public String name() { return "dHash"; }
}
