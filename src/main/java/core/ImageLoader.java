package core;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Path;

public final class ImageLoader {
    public static BufferedImage load(Path path) throws Exception {
        var img = ImageIO.read(path.toFile());
        if (img == null) {
            throw new IllegalArgumentException("Unsupported image format: " + path);
        }
        try {
            return core.Exif.applyOrientation(img, path);
        } catch (Exception e) {
            System.err.println("Warning: Could not apply EXIF orientation for " + path + ". Using original orientation. Reason: " + e.getMessage());
            return img; // Return the original, un-rotated image
        }
    }
}
