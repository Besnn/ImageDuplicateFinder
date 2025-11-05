package hash;
import core.Gray;
import core.Resize;
import java.awt.image.BufferedImage;

public class DHash implements Hasher {
    public long hash(BufferedImage img) {
        // Preprocess: convert to grayscale and resize
        img = Gray.toGray(img);
        img = Resize.resize(img, 9, 8);
        
        int w = 9, h = 8;
        long bits = 0L;
        int i=0;
        for (int y=0;y<h;y++) {
            for (int x=0;x<w-1;x++) {
                int rgbLeft = img.getRGB(x, y);
                int r = (rgbLeft >> 16) & 0xff;
                int g = (rgbLeft >> 8) & 0xff;
                int b = rgbLeft & 0xff;
                int left = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                
                int rgbRight = img.getRGB(x+1, y);
                r = (rgbRight >> 16) & 0xff;
                g = (rgbRight >> 8) & 0xff;
                b = rgbRight & 0xff;
                int right = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                
                if (left > right) bits |= (1L << i);
                i++;
            }
        }
        return bits;
    }
    public String name() { return "dHash"; }
}
