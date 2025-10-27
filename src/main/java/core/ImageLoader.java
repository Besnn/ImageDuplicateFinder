package core;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Path;

public final class ImageLoader {
    public static BufferedImage load(Path path) throws Exception {
        var img = ImageIO.read(path.toFile());
        if (img == null) throw new IllegalArgumentException("Unsupported format: " + path);
        return core.Exif.applyOrientation(img, path);
    }
}
