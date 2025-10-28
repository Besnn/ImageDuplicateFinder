package core;// java

import core.Exif;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

public class ExifDebug {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("C:\\Users\\besnn\\projects\\ImageDuplicateFinder\\img\\drink-270cw.jpg");
        BufferedImage before = ImageIO.read(p.toFile());
        int orientation = Exif.readOrientationSafe(p);
        BufferedImage after = Exif.applyOrientation(before, p);

        System.out.printf("path=%s%n", p);
        System.out.printf("orientation=%d%n", orientation);
        System.out.printf("before WxH=%dx%d%n", before.getWidth(), before.getHeight());
        System.out.printf("after  WxH=%dx%d%n", after.getWidth(), after.getHeight());

        ImageIO.write(before, "jpg", new File("debug-before.jpg"));
        ImageIO.write(after, "jpg", new File("debug-after.jpg"));
        System.out.println("Wrote debug-before.jpg and debug-after.jpg");
    }
}
