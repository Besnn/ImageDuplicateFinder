package hash;
import java.awt.image.BufferedImage;

public interface Hasher {
    long hash(BufferedImage img);
    String name();
}
