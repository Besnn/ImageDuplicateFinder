package core;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Applies EXIF orientation (if present) to a BufferedImage.
 * If no orientation tag is present, returns the original image.
 *
 * Orientation mapping (EXIF tag 274):
 * 1 = 0°               (no-op)
 * 2 = mirror horizontal
 * 3 = rotate 180°
 * 4 = mirror vertical
 * 5 = mirror horizontal + rotate 270° (transpose)
 * 6 = rotate 90° CW
 * 7 = mirror horizontal + rotate 90°  (transverse)
 * 8 = rotate 270° CW
 */
public final class Exif {

    private Exif() {}

    /** Convenience: load orientation from file path and apply. */
    public static BufferedImage applyOrientation(BufferedImage img, Path imagePath) {
        int o = readOrientationSafe(imagePath);
        return transform(img, o);
    }

    /** Read EXIF orientation (1..8). Returns 1 if missing/unknown. */
    public static int readOrientationSafe(Path imagePath) {
        if (imagePath == null || !Files.isRegularFile(imagePath)) return 1;
        try {
            Metadata md = ImageMetadataReader.readMetadata(imagePath.toFile());
            ExifIFD0Directory dir = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                Integer v = dir.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
                if (v != null && v >= 1 && v <= 8) return v;
            }
        } catch (Exception ignore) {
            // Non-fatal: treat as no-orientation
        }
        return 1;
    }

    /** Apply EXIF orientation (1..8). Unknown values are treated as 1. */
    public static BufferedImage transform(BufferedImage src, int orientation) {
        if (src == null) return null;
        return switch (orientation) {
            case 1 -> src;
            case 2 -> flipHorizontal(src);
            case 3 -> rotate180(src);
            case 4 -> flipVertical(src);
            case 5 -> transpose(src);   // mirror horizontal + rotate 270
            case 6 -> rotate90(src);
            case 7 -> transverse(src);  // mirror horizontal + rotate 90
            case 8 -> rotate270(src);
            default -> src;
        };
    }

    /* ---- transforms ---- */

    private static BufferedImage rotate90(BufferedImage src) {
        AffineTransform at = new AffineTransform();
        at.translate(src.getHeight(), 0);
        at.rotate(Math.toRadians(90));
        return transformAffine(src, at, src.getHeight(), src.getWidth());
    }

    private static BufferedImage rotate180(BufferedImage src) {
        AffineTransform at = new AffineTransform();
        at.translate(src.getWidth(), src.getHeight());
        at.rotate(Math.toRadians(180));
        return transformAffine(src, at, src.getWidth(), src.getHeight());
    }

    private static BufferedImage rotate270(BufferedImage src) {
        AffineTransform at = new AffineTransform();
        at.translate(0, src.getWidth());
        at.rotate(Math.toRadians(270));
        return transformAffine(src, at, src.getHeight(), src.getWidth());
    }

    private static BufferedImage flipHorizontal(BufferedImage src) {
        AffineTransform at = new AffineTransform(-1, 0, 0, 1, src.getWidth(), 0);
        return transformAffine(src, at, src.getWidth(), src.getHeight());
    }

    private static BufferedImage flipVertical(BufferedImage src) {
        AffineTransform at = new AffineTransform(1, 0, 0, -1, 0, src.getHeight());
        return transformAffine(src, at, src.getWidth(), src.getHeight());
    }

    // Orientation 5: transpose (mirror across TL–BR)
    private static BufferedImage transpose(BufferedImage src) {
        // Equivalent: flip horizontal then rotate 270
        return rotate270(flipHorizontal(src));
    }

    // Orientation 7: transverse (mirror across TR–BL)
    private static BufferedImage transverse(BufferedImage src) {
        // Equivalent: flip horizontal then rotate 90
        return rotate90(flipHorizontal(src));
    }

    private static BufferedImage transformAffine(BufferedImage src, AffineTransform at, int outW, int outH) {
        BufferedImage dst = new BufferedImage(outW, outH, chooseType(src));
        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        op.filter(src, dst);
        return dst;
    }

    /** Preserve alpha if present; otherwise use 3-byte BGR. */
    private static int chooseType(BufferedImage src) {
        return src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_3BYTE_BGR;
    }

    /** Optional: high-quality resample helper if you later resize after EXIF fix. */
    public static BufferedImage resample(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, chooseType(src));
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return dst;
    }
}

//TODO: test this functionality
// Tests I should write:
// No EXIF → image unchanged.
// Each orientation 1..8 → pixel coordinates map correctly (use tiny fixtures: arrows or “L” shapes).
// Alpha preservation: input with transparency → output still has alpha.
// Non-JPEG / corrupted EXIF → no exceptions, orientation=1.
