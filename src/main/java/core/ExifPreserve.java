package core;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ExifPreserve {
    public static void writeJpegPreserveExif(File originalFile, BufferedImage image, File outFile) throws Exception {
        // encode BufferedImage to JPEG bytes
        byte[] jpegBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            baos.flush();
            jpegBytes = baos.toByteArray();
        }

        // obtain EXIF from the original file (if present)
        TiffOutputSet outputSet = null;
        ImageMetadata metadata = Imaging.getMetadata(originalFile);
        if (metadata instanceof JpegImageMetadata) {
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            TiffImageMetadata exif = jpegMetadata.getExif();
            if (exif != null) {
                outputSet = exif.getOutputSet();
            }
        }
        if (outputSet == null) {
            outputSet = new TiffOutputSet(); // empty set if original had no EXIF
        }

        // write final JPEG with EXIF inserted
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            new ExifRewriter().updateExifMetadataLossless(jpegBytes, fos, outputSet);
        }
    }
}
