package app;

import picocli.CommandLine.*;
import core.*;
import hash.*;
import index.*;
import cluster.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.Callable;

//TODO: work on subcommands

public final class Commands {

    @Command(
            name = "hash",
            description = "Compute perceptual hashes for images under ROOT.",
            mixinStandardHelpOptions = true
    )
    public static class Hash implements Callable<Integer> {

        @Parameters(index = "0", paramLabel = "ROOT", description = "Root folder to scan")
        Path root;

        @Option(names = "--algo", defaultValue = "phash", description = "Hasher: ahash|dhash|phash")
        String algo;

        @Option(names = "--out", defaultValue = "index.csv", description = "Output index file (CSV)")
        Path out;

        @Override
        public Integer call() {
            try {
                Hasher hasher = switch (algo.toLowerCase()) {
                    case "ahash" -> new AHash();
                    case "dhash" -> new DHash();
                    case "phash" -> new PHashDct();
                    default -> {
                        System.err.println("Unknown --algo: " + algo);
                        yield null;
                    }
                };
                if (hasher == null) return CLI.Exit.USAGE;

                Index index = new BKTreeIndex();
                Map<String, Long> id2hash = new LinkedHashMap<>();

                try (var paths = Files.walk(root)) {
                    paths.filter(Files::isRegularFile)
                            .filter(p -> p.toString().matches("(?i).*\\.(jpg|jpeg|png|bmp)"))
                            .forEach(p -> {
                                try {
                                    BufferedImage img = ImageLoader.load(p);
                                    img = Gray.toGray(img);
                                    // size for hasher:
                                    img = hasher instanceof PHashDct ? Resize.resize(img, PHASH_SIZE, PHASH_SIZE)
                                            : Resize.resize(img, DHASH_WIDTH, DHASH_HEIGHT); // dhash needs 9x8, ahash 8x8; 9x8 is safe and we only sample 8x8 inside
                                    long h = hasher.hash(img);
                                    index.add(h, p.toString());
                                    id2hash.put(p.toString(), h);
                                } catch (Exception e) {
                                    System.err.println("Skip " + p + " (" + e.getMessage() + ")");
                                }
                            });
                }

                // persist simple CSV: path, unsignedHash
                var lines = id2hash.entrySet().stream()
                        .map(e -> e.getKey() + "," + Long.toUnsignedString(e.getValue()))
                        .toList();
                Files.write(out, lines);

                System.out.printf("Hashed %d images with %s -> %s%n", id2hash.size(), hasher.name(), out);
                return CLI.Exit.OK;

            } catch (NoSuchFileException e) {
                System.err.println("No such file or directory: " + e.getFile());
                return CLI.Exit.USAGE;
            } catch (Exception e) {
                e.printStackTrace();
                return CLI.Exit.RUNTIME_ERROR;
            }
        }
        private static final int PHASH_SIZE = 32;
        private static final int DHASH_WIDTH = 9;
        private static final int DHASH_HEIGHT = 8;
    }

    @Command(
            name = "cluster",
            description = "Cluster near-duplicates from an index CSV.",
            mixinStandardHelpOptions = true
    )
    public static class Cluster implements Callable<Integer> {

        @Parameters(index = "0", paramLabel = "INDEX", description = "CSV produced by 'hash' (path,hash)")
        Path indexCsv;

        @Option(names = "--radius", defaultValue = "10", description = "Hamming radius (0..64)")
        int radius;

        @Option(names = "--out", defaultValue = "clusters.csv", description = "Output clusters CSV")
        Path out;

        @Override
        public Integer call() {
            try {
                // load index
                Map<String, Long> id2hash = new LinkedHashMap<>();
                BKTreeIndex idx = new BKTreeIndex();

                for (String line : Files.readAllLines(indexCsv)) {
                    if (line.isBlank()) continue;
                    int comma = line.lastIndexOf(',');
                    if (comma <= 0) continue;
                    String path = line.substring(0, comma);
                    long hash = Long.parseUnsignedLong(line.substring(comma + 1));
                    id2hash.put(path, hash);
                    idx.add(hash, path);
                }

                var clusters = Clusterer.cluster(id2hash, idx, radius);

                // Write clusters: clusterId,path
                List<String> rows = new ArrayList<>();
                for (var c : clusters) {
                    for (String member : c.members()) {
                        rows.add(c.id() + "," + member);
                    }
                }
                Files.write(out, rows);
                System.out.printf("Clusters: %d written -> %s%n", clusters.size(), out);
                return CLI.Exit.OK;

            } catch (NoSuchFileException e) {
                System.err.println("Index file not found: " + e.getFile());
                return CLI.Exit.USAGE;
            } catch (Exception e) {
                e.printStackTrace();
                return CLI.Exit.RUNTIME_ERROR;
            }
        }
    }
}

//TODO: what should I do with cluster.csv?
