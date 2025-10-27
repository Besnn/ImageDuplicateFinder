package app;
import picocli.CommandLine.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import core.*;
import hash.*;
import index.*;

public final class Commands {

    @Command(name="hash", description="Compute perceptual hashes")
    public static class Hash implements Runnable {
        @Parameters(index="0", description="Root folder") Path root;
        @Option(names="--hasher", defaultValue="phash", description="Hasher type") String algo;
        @Option(names="--out", defaultValue="index.json") Path out;

        public void run() {
            Hasher h = switch (algo.toLowerCase()) {
                case "ahash" -> new AHash();
                case "dhash" -> new DHash();
                default -> new PHashDct();
            };
            Index index = new BKTreeIndex();
            Map<String,Long> map = new HashMap<>();
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().matches(".*\\.(?i)(jpg|jpeg|png|bmp)"))
                        .forEach(p -> {
                            try {
                                BufferedImage img = ImageLoader.load(p);
                                img = Gray.toGray(img);
                                img = Resize.resize(img, 32, 32);
                                long hash = h.hash(img);
                                index.add(hash, p.toString());
                                map.put(p.toString(), hash);
                            } catch (Exception e) {
                                System.err.println("Skip "+p+" ("+e+")");
                            }
                        });
            } catch (Exception e) { e.printStackTrace(); }
            System.out.printf("Hashed %d images with %s%n", map.size(), h.name());
            // TODO: persist map/index
        }
    }

    // Similar subcommand: cluster
}
