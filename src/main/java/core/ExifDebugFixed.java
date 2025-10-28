package core;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

public class ExifDebugFixed {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("C:\\Users\\besnn\\projects\\ImageDuplicateFinder\\img\\drink-270cw.jpg");
        File f = p.toFile();

        BufferedImage before = ImageIO.read(f);

        Metadata md = ImageMetadataReader.readMetadata(f);
        ExifIFD0Directory exif = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
        JpegDirectory jpeg = md.getFirstDirectoryOfType(JpegDirectory.class);

        int orientation = 1;
        if (exif != null && exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
            orientation = exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        }

        int fileW = jpeg != null ? jpeg.getImageWidth() : -1;
        int fileH = jpeg != null ? jpeg.getImageHeight() : -1;

        System.out.printf("path=%s%n", p);
        System.out.printf("orientation=%d%n", orientation);
        System.out.printf("stored file WxH=%dx%d%n", fileW, fileH);
        System.out.printf("before  BufferedImage WxH=%dx%d%n", before.getWidth(), before.getHeight());

        boolean likelyAutoRotated = false;
        if (orientation >= 5 && orientation <= 8 && fileW > 0 && fileH > 0) {
            // if stored file WxH are swapped relative to the BufferedImage, the reader likely auto-rotated
            likelyAutoRotated = (before.getWidth() == fileH && before.getHeight() == fileW);
        }

        System.out.println("likelyAutoRotated=" + likelyAutoRotated);

        BufferedImage after = likelyAutoRotated ? before : Exif.applyOrientation(before, p);

        ImageIO.write(before, "jpg", new File("debug-before.jpg"));
        ImageIO.write(after, "jpg", new File("debug-after.jpg"));
        System.out.println("Wrote `debug-before.jpg` and `debug-after.jpg`");
    }
}
