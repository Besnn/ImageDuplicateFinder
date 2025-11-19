package app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandsTest {

    @TempDir
    Path tempDir;

    private Path rootDir;
    private Path quarantineDir;

    // A simple 1x1 black pixel PNG
    private static final byte[] PNG_DATA = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, (byte) 0x77,
            0x53, (byte) 0xde, 0x00, 0x00, 0x00, 0x0c, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xd7, 0x63, 0x60, 0x60,
            0x60, 0x00, 0x00, 0x00, 0x04, 0x00, 0x01, (byte) 0x95, 0x30, (byte) 0xe1, (byte) 0xb8, 0x00, 0x00, 0x00,
            0x00, 0x49, 0x45, 0x4e, 0x44, (byte) 0xae, 0x42, 0x60, (byte) 0x82
    };

    // A simple 1x1 white pixel PNG
    private static final byte[] PNG_DATA_WHITE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, (byte) 0xc4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0a, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xd7, 0x63, (byte) 0xf8,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x3f, 0x00, 0x05, (byte) 0xfe, 0x02, (byte) 0xfe,
            (byte) 0xdc, (byte) 0xcc, 0x59, (byte) 0xe7, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44, (byte) 0xae,
            0x42, 0x60, (byte) 0x82
    };

    @BeforeEach
    void setUp() throws IOException {
        rootDir = tempDir.resolve("images");
        quarantineDir = tempDir.resolve("quarantine");
        Files.createDirectories(rootDir);
        Files.createDirectories(quarantineDir);
    }

    private Path createImage(String name, byte[] data) throws IOException {
        Path imgPath = rootDir.resolve(name);
        Files.createDirectories(imgPath.getParent()); // Ensure parent directory exists
        Files.write(imgPath, data);
        return imgPath;
    }

    private Path createImageWithPixels(String name, int width, int height) throws IOException {
        Path imgPath = rootDir.resolve(name);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", imgPath.toFile());
        return imgPath;
    }

    @Test
    void hashCommand_Success() throws Exception {
        createImage("img1.png", PNG_DATA);
        createImage("img2.jpg", PNG_DATA);
        Files.createFile(rootDir.resolve("not_an_image.txt"));

        Commands.Hash hashCommand = new Commands.Hash();
        hashCommand.root = rootDir;
        hashCommand.out = tempDir.resolve("index.csv");
        hashCommand.algo = "phash";

        Integer exitCode = hashCommand.call();

        assertEquals(CommandLine.ExitCode.OK, exitCode, "Hash command should exit with OK");
        assertTrue(Files.exists(hashCommand.out), "Output index file should be created");

        List<String> lines = Files.readAllLines(hashCommand.out);
        assertEquals(2, lines.size(), "Index file should contain 2 entries");
        assertTrue(lines.stream().anyMatch(s -> s.contains("img1.png")), "Index should contain img1.png");
        assertTrue(lines.stream().anyMatch(s -> s.contains("img2.jpg")), "Index should contain img2.jpg");
    }

    @Test
    void hashCommand_InvalidAlgo() {
        Commands.Hash hashCommand = new Commands.Hash();
        hashCommand.root = rootDir;
        hashCommand.out = tempDir.resolve("index.csv");
        hashCommand.algo = "unknown-algo";

        Integer exitCode = hashCommand.call();

        assertEquals(CommandLine.ExitCode.USAGE, exitCode, "Hash command should exit with USAGE for unknown algorithm");
    }

    @Test
    void hashCommand_NonExistentRoot() {
        Commands.Hash hashCommand = new Commands.Hash();
        hashCommand.root = tempDir.resolve("non-existent-dir");
        hashCommand.out = tempDir.resolve("index.csv");
        hashCommand.algo = "phash";

        Integer exitCode = hashCommand.call();
        assertEquals(CommandLine.ExitCode.USAGE, exitCode, "Hash command should exit with USAGE for non-existent root");
    }

    @Test
    void clusterCommand_Success() throws Exception {
        Path indexCsv = tempDir.resolve("index.csv");
        Path img1Path = createImage("img1.png", PNG_DATA);
        Path img2Path = createImage("img2.png", PNG_DATA); // Identical image
        Path img3Path = createImage("img3.png", PNG_DATA_WHITE); // Different image

        List<String> indexLines = Arrays.asList(
                img1Path + ",0", // hash for black 1x1 phash
                img2Path + ",0",
                img3Path + "," + Long.toUnsignedString(-1L) // hash for white 1x1 phash
        );
        Files.write(indexCsv, indexLines);

        Commands.Cluster clusterCommand = new Commands.Cluster();
        clusterCommand.indexCsv = indexCsv;
        clusterCommand.out = tempDir.resolve("clusters.csv");
        clusterCommand.radius = 5;

        Integer exitCode = clusterCommand.call();

        assertEquals(CommandLine.ExitCode.OK, exitCode, "Cluster command should exit with OK");
        assertTrue(Files.exists(clusterCommand.out), "Output clusters file should be created");

        List<String> clusterLines = Files.readAllLines(clusterCommand.out);
        // Expect one cluster with 2 members, and one with 1 member.
        // The single-member cluster is omitted by the current implementation.
        assertEquals(2, clusterLines.size(), "Should find one cluster with two members");
        String clusterId = clusterLines.get(0).split(",")[0];
        assertTrue(clusterLines.stream().allMatch(line -> line.startsWith(clusterId + ",")), "All members should be in the same cluster");
        assertTrue(clusterLines.stream().anyMatch(line -> line.endsWith("img1.png")), "Cluster should contain img1.png");
        assertTrue(clusterLines.stream().anyMatch(line -> line.endsWith("img2.png")), "Cluster should contain img2.png");
    }

    @Test
    void clusterCommand_IndexNotFound() {
        Commands.Cluster clusterCommand = new Commands.Cluster();
        clusterCommand.indexCsv = tempDir.resolve("non-existent.csv");
        clusterCommand.out = tempDir.resolve("clusters.csv");

        Integer exitCode = clusterCommand.call();
        assertEquals(CommandLine.ExitCode.USAGE, exitCode, "Cluster command should exit with USAGE for non-existent index file");
    }

    @Test
    void planCommand_Success() throws Exception {
        Path clustersCsv = tempDir.resolve("clusters.csv");

        // Create files with different metadata to test sorting logic
        Path keeperPath = createImageWithPixels("keeper.png", 200, 200); // More pixels
        Files.setLastModifiedTime(keeperPath, java.nio.file.attribute.FileTime.fromMillis(1000));

        Path dupe1Path = createImageWithPixels("dupe1.png", 100, 100); // Less pixels
        Files.setLastModifiedTime(dupe1Path, java.nio.file.attribute.FileTime.fromMillis(2000));

        // Create a valid but large image file to test size comparison
        Path dupe2Path = rootDir.resolve("dupe2.png");
        ImageIO.write(new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB), "png", dupe2Path.toFile());
        Files.setLastModifiedTime(dupe2Path, java.nio.file.attribute.FileTime.fromMillis(3000));

        List<String> clusterLines = Arrays.asList(
                "cluster1," + keeperPath,
                "cluster1," + dupe1Path,
                "cluster1," + dupe2Path
        );
        Files.write(clustersCsv, clusterLines);

        Commands.Plan planCommand = new Commands.Plan();
        planCommand.clustersCsv = clustersCsv;
        planCommand.out = tempDir.resolve("plan.csv");

        Integer exitCode = planCommand.call();

        assertEquals(0, exitCode, "Plan command should exit with 0");
        assertTrue(Files.exists(planCommand.out), "Output plan file should be created");

        List<String> planLines = Files.readAllLines(planCommand.out);
        assertEquals(4, planLines.size(), "Plan file should have a header and 3 data rows");
        assertTrue(planLines.get(1).contains("KEEP," + dupe2Path), "dupe2.png should be marked as KEEP");
        assertTrue(planLines.get(2).contains("DELETE," + keeperPath), "keeper.png should be marked as DELETE");
        assertTrue(planLines.get(3).contains("DELETE," + dupe1Path), "dupe1.png should be marked as DELETE");
    }

    @Test
    void applyCommand_MoveToQuarantine() throws Exception {
        Path planCsv = tempDir.resolve("plan.csv");
        Path keeperFile = createImage("keeper.png", PNG_DATA);
        Path deleteFile = createImage("to_delete.png", PNG_DATA);

        List<String> planLines = Arrays.asList(
                "clusterId,action,path,reason",
                "c1,KEEP," + keeperFile + ",reason",
                "c1,DELETE," + deleteFile + ",reason"
        );
        Files.write(planCsv, planLines);

        Commands.Apply applyCommand = new Commands.Apply();
        applyCommand.planCsv = planCsv;
        applyCommand.quarantine = quarantineDir;
        applyCommand.hardlink = false;

        Integer exitCode = applyCommand.call();

        assertEquals(0, exitCode, "Apply command should exit with 0");
        assertTrue(Files.exists(keeperFile), "Keeper file should remain in place");
        assertFalse(Files.exists(deleteFile), "File to delete should be moved");
        assertTrue(Files.exists(quarantineDir.resolve(deleteFile.getFileName())), "Deleted file should be in quarantine");
    }

    @Test
    void applyCommand_HandleDuplicateQuarantineNames() throws Exception {
        Path planCsv = tempDir.resolve("plan.csv");
        Path file1 = createImage("file.png", PNG_DATA); // Creates .../images/file.png
        Path file2 = createImage("another_dir/file.png", PNG_DATA); // Creates .../images/another_dir/file.png

        // Pre-create a file in quarantine to force renaming
        Files.createFile(quarantineDir.resolve("file.png"));

        List<String> planLines = Arrays.asList(
                "clusterId,action,path,reason",
                "c1,DELETE," + file1 + ",reason",
                "c1,DELETE," + file2 + ",reason"
        );
        Files.write(planCsv, planLines);

        Commands.Apply applyCommand = new Commands.Apply();
        applyCommand.planCsv = planCsv;
        applyCommand.quarantine = quarantineDir;

        Integer exitCode = applyCommand.call();

        assertEquals(0, exitCode, "Apply command should exit with 0");
        assertFalse(Files.exists(file1), "File1 should be moved");
        assertFalse(Files.exists(file2), "File2 should be moved");

        assertTrue(Files.exists(quarantineDir.resolve("file.png")), "Original quarantine file should still exist");
        assertTrue(Files.exists(quarantineDir.resolve("file_1.png")), "First moved file should be renamed");
        assertTrue(Files.exists(quarantineDir.resolve("file_2.png")), "Second moved file should be renamed");
    }

    @Test
    void applyCommand_Hardlink() throws Exception {
        // Hardlink tests are tricky to make platform-independent and may fail on filesystems
        // that don't support them (like FAT32). We check for support by attempting to create one.
        try {
            Path target = tempDir.resolve("target.tmp");
            Path link = tempDir.resolve("link.tmp");
            Files.createFile(target);
            Files.createLink(link, target);
            Files.delete(link);
            Files.delete(target);
        } catch (UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.abort("Skipping hardlink test: filesystem does not support hard links.");
        }

        Path planCsv = tempDir.resolve("plan.csv");
        Path keeperFile = createImage("keeper.png", PNG_DATA);
        Path deleteFile = createImage("to_delete.png", PNG_DATA);

        List<String> planLines = Arrays.asList(
                "clusterId,action,path,reason",
                "c1,KEEP," + keeperFile + ",reason",
                "c1,DELETE," + deleteFile + ",reason"
        );
        Files.write(planCsv, planLines);

        Commands.Apply applyCommand = new Commands.Apply();
        applyCommand.planCsv = planCsv;
        applyCommand.quarantine = quarantineDir;
        applyCommand.hardlink = true;

        Integer exitCode = applyCommand.call();

        assertEquals(0, exitCode, "Apply command should exit with 0");
        assertTrue(Files.exists(keeperFile), "Keeper file should exist");
        // The current implementation moves to quarantine even with --hardlink
        assertFalse(Files.exists(deleteFile), "DELETE file should be moved, not hardlinked in place");
        assertTrue(Files.exists(quarantineDir.resolve(deleteFile.getFileName())), "DELETE file should be in quarantine");
    }
}
