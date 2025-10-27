package core;
import java.awt.image.BufferedImage;
import java.awt.Color;

public final class Gray {
    public static BufferedImage toGray(BufferedImage src) {
        BufferedImage g = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        g.getGraphics().drawImage(src, 0, 0, null);
        return g;
    }
}
