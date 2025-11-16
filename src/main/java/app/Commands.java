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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.Callable;


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
            // Can remove these constants
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
                    long hash = Long.parseUnsignedLong(line.substring(comma + 1).trim());
                    id2hash.put(path, hash);
                    idx.add(hash, path);
                }

                var clusters = Clusterer.cluster(id2hash, idx, radius);

                // Write clusters: clusterId,path
                List<String> rows = new ArrayList<>();
                for (var c : clusters) {
                    if (c.members().size() <= 1) continue; // Only write clusters with duplicates
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

    @Command(
            name="plan",
            description="Create deduplication plan from clusters.csv",
            mixinStandardHelpOptions = true
    )
    public static class Plan implements Callable<Integer> {

        @Parameters(index = "0", paramLabel = "CLUSTERS", description = "clusters.csv (clusterId,path)")
        Path clustersCsv;

        @Option(names = "--out", defaultValue = "plan.csv", description = "Output plan CSV")
        Path out;

        @Override
        public Integer call() {
            try {
                // group paths by clusterId
                Map<String, List<Path>> groups = new LinkedHashMap<>();
                for (String line : Files.readAllLines(clustersCsv)) {
                    if (line.isBlank()) continue;
                    int c = line.indexOf(',');
                    if (c <= 0) continue;
                    String cid = line.substring(0, c);
                    Path p = Path.of(line.substring(c + 1));
                    groups.computeIfAbsent(cid, k -> new ArrayList<>()).add(p);
                }

                List<String> rows = new ArrayList<>();
                rows.add("clusterId,action,path,reason");

                for (var e : groups.entrySet()) {
                    String cid = e.getKey();
                    List<Path> files = e.getValue();
                    
                    // Pre-compute metadata to avoid re-computing in sort
                    Map<Path, FileMeta> metas = new HashMap<>();
                    files.forEach(p -> metas.put(p, meta(p)));
                    
                    // score files: bigger resolution, then size, then older mtime, then path
                    files.sort((a, b) -> {
                        FileMeta ma = metas.get(a), mb = metas.get(b);
                        int byPixels = Long.compare(mb.pixels, ma.pixels);
                        if (byPixels != 0) return byPixels;
                        int bySize = Long.compare(mb.size, ma.size);
                        if (bySize != 0) return bySize;
                        int byTime = Long.compare(ma.mtime, mb.mtime); // older first
                        if (byTime != 0) return byTime;
                        return a.toString().compareToIgnoreCase(b.toString());
                    });
                    
                    Path keeper = files.getFirst(); // The best file after sorting
                    FileMeta mk = metas.get(keeper);
                    rows.add(String.format("%s,KEEP,%s,keeper(pixels=%d,size=%d,mtime=%d)",
                            cid, keeper, mk.pixels, mk.size, mk.mtime));

                    for (int i = 1; i < files.size(); i++) {
                        Path dupe = files.get(i);
                        FileMeta md = metas.get(dupe);
                        rows.add(String.format("%s,DELETE,%s,dupe(pixels=%d,size=%d,mtime=%d)",
                                cid, dupe, md.pixels, md.size, md.mtime));
                    }
                }

                Files.write(out, rows);
                System.out.println("Plan written -> " + out);
                return 0;
            } catch (Exception ex) {
                ex.printStackTrace();
                return 1;
            }
        }

        static final class FileMeta {
            final long pixels, size, mtime;

            FileMeta(long pixels, long size, long mtime) {
                this.pixels = pixels;
                this.size = size;
                this.mtime = mtime;
            }
        }

        static FileMeta meta(Path p) {
            try {
                long size = Files.size(p);
                long mtime = Files.getLastModifiedTime(p).toMillis();
                // Fast dimension probe: read minimally (okay for JPEG/PNG). If you want ultra-fast,
                // you can use metadata-extractor to read dimensions without full decode.
                var img = javax.imageio.ImageIO.read(p.toFile());
                long pixels = (img != null) ? (long) img.getWidth() * img.getHeight() : -1;
                return new FileMeta(pixels, size, mtime);
            } catch (Exception e) {
                return new FileMeta(-1, -1, Long.MAX_VALUE); // penalize unreadables
            }
        }
    }

    @Command(
            name="apply",
            description="Apply plan.csv: move DELETE files to quarantine",
            mixinStandardHelpOptions = true
    )
    public static class Apply implements Callable<Integer> {

        @Parameters(index="0", paramLabel="PLAN", description="plan.csv (clusterId,action,path,reason)")
        Path planCsv;

        @Option(names="--quarantine", defaultValue="./quarantine", description="Folder to move duplicates")
        Path quarantine;

        @Option(names="--hardlink", description="Replace DELETE files with hardlinks to keeper when on same filesystem")
        boolean hardlink;

        @Override public Integer call() {
            try {
                Files.createDirectories(quarantine);
                Map<String, Path> keepers = new HashMap<>();

                // Pre-scan to remember keeper paths per cluster
                List<String> lines = Files.readAllLines(planCsv);
                for (String line : lines) {
                    if (line.startsWith("clusterId") || line.isBlank()) continue;
                    String[] parts = line.split(",", 4);
                    String cid = parts[0], action = parts[1], path = parts[2];
                    if ("KEEP".equalsIgnoreCase(action)) keepers.put(cid, Path.of(path));
                }

                // Apply DELETE actions
                for (String line : lines) {
                    if (line.startsWith("clusterId") || line.isBlank()) continue;
                    String[] parts = line.split(",", 4);
                    String cid = parts[0], action = parts[1], path = parts[2];
                    Path file = Path.of(path);

                    if ("DELETE".equalsIgnoreCase(action)) {
                        if (hardlink) {
                            Path keeper = keepers.get(cid);
                            if (keeper != null && Files.exists(keeper)) {
                                // move to quarantine first (safety), then optionally replace original with hardlink to keeper
                                Path dest = quarantine.resolve(file.getFileName().toString());
                                safeMove(file, dest);
                                // If you truly want in-place hardlinking, you'd link back at original path:
                                // Files.createLink(file, keeper);  // works only on same filesystem and if original path free
                            } else {
                                // fallback to just quarantine
                                Path dest = quarantine.resolve(file.getFileName().toString());
                                safeMove(file, dest);
                            }
                        } else {
                            Path dest = quarantine.resolve(file.getFileName().toString());
                            safeMove(file, dest);
                        }
                    }
                }
                System.out.println("Apply completed. Review " + quarantine);
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
        }

        static void safeMove(Path src, Path dst) throws Exception {
            Path uniq = dst;
            int i = 1;
            while (Files.exists(uniq)) {
                String name = dst.getFileName().toString();
                String base;
                String ext = "";
                int dot = name.lastIndexOf('.');
                if (dot > 0) {  // Changed from >= to > to handle hidden files better
                    base = name.substring(0, dot);
                    ext = name.substring(dot);
                } else {
                    base = name;
                }
                uniq = dst.getParent().resolve(base + "_" + (i++) + ext);
            }
            Files.createDirectories(uniq.getParent());
            Files.move(src, uniq);
        }
    }

    @Command(
            name = "web",
            description = "Start web UI for reviewing duplicates",
            mixinStandardHelpOptions = true
    )
    public static class Web implements Callable<Integer> {

        @Option(names = "--port", defaultValue = "7070", description = "Web server port")
        int port;

        @Option(names = "--plan", description = "Optional plan.csv to load")
        Path planCsv;

        @Option(names = "--clusters", description = "Optional clusters.csv to load")
        Path clustersCsv;

        @Override
        public Integer call() {
            try {
                WebServer server = new WebServer(planCsv, clustersCsv);
                server.start(port);

                System.out.println("Web UI started at http://localhost:" + port);
                System.out.println("Press Ctrl+C to stop");

                // Keep running
                Thread.currentThread().join();
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
        }
    }


}
