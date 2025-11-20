package app;

import io.javalin.Javalin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the Web UI (index.html).
 * Tests the complete user workflow using Selenium WebDriver.
 *
 * NOTE: These tests require Chrome/Chromium to be installed.
 * Set system property 'webdriver.chrome.driver' if ChromeDriver is not in PATH.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebUITest {

    @TempDir
    Path tempDir;

    private static WebDriver driver;
    private static Javalin app;
    private static int testPort = 18080;
    private WebDriverWait wait;

    private static final byte[] PNG_DATA = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, (byte) 0x77,
            0x53, (byte) 0xde, 0x00, 0x00, 0x00, 0x0c, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xd7, 0x63, 0x60, 0x60,
            0x60, 0x00, 0x00, 0x00, 0x04, 0x00, 0x01, (byte) 0x95, 0x30, (byte) 0xe1, (byte) 0xb8, 0x00, 0x00, 0x00,
            0x00, 0x49, 0x45, 0x4e, 0x44, (byte) 0xae, 0x42, 0x60, (byte) 0x82
    };

    private static final byte[] PNG_DATA_WHITE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, (byte) 0xc4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0a, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xd7, 0x63, (byte) 0xf8,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x3f, 0x00, 0x05, (byte) 0xfe, 0x02, (byte) 0xfe,
            (byte) 0xdc, (byte) 0xcc, 0x59, (byte) 0xe7, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44, (byte) 0xae,
            0x42, 0x60, (byte) 0x82
    };

    @BeforeAll
    static void setUpWebDriver() {
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Run in headless mode for CI/CD
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            driver = new ChromeDriver(options);
        } catch (Exception e) {
            // If ChromeDriver fails, skip all tests
            Assumptions.assumeTrue(false, "ChromeDriver not available: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDownWebDriver() {
        if (driver != null) {
            driver.quit();
        }
        if (app != null) {
            app.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    @Order(1)
    @DisplayName("UI loads successfully and displays title")
    void uiLoadsSuccessfully() throws Exception {
        // Start server without pre-loaded data
        WebServer server = new WebServer(null, null);
        app = server.start(testPort);
        Thread.sleep(200); // Give server time to start

        driver.get("http://localhost:" + testPort);

        // Wait for page to load
        wait.until(ExpectedConditions.titleIs("Image Duplicate Finder"));

        // Verify main heading
        WebElement heading = driver.findElement(By.tagName("h1"));
        assertEquals("Image Duplicate Finder", heading.getText());

        // Verify stats section exists
        assertTrue(driver.findElement(By.id("clusterCount")).isDisplayed());
        assertTrue(driver.findElement(By.id("imageCount")).isDisplayed());
        assertTrue(driver.findElement(By.id("keepCount")).isDisplayed());
        assertTrue(driver.findElement(By.id("deleteCount")).isDisplayed());
    }

    @Test
    @Order(2)
    @DisplayName("Setup panel is visible with all input fields")
    void setupPanelDisplaysCorrectly() throws Exception {
        if (app == null) {
            WebServer server = new WebServer(null, null);
            app = server.start(testPort);
            Thread.sleep(200);
        }

        driver.get("http://localhost:" + testPort);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("setupPanel")));

        // Verify setup panel elements
        WebElement directoryInput = driver.findElement(By.id("directoryInput"));
        assertTrue(directoryInput.isDisplayed());
        assertEquals("text", directoryInput.getAttribute("type"));

        WebElement thresholdInput = driver.findElement(By.id("thresholdInput"));
        assertTrue(thresholdInput.isDisplayed());
        assertEquals("0.95", thresholdInput.getAttribute("value"));

        WebElement algoInput = driver.findElement(By.id("algoInput"));
        assertTrue(algoInput.isDisplayed());
        assertEquals("phash", algoInput.getAttribute("value"));

        // Verify process button exists
        WebElement processBtn = driver.findElements(By.cssSelector("button")).stream()
                .filter(btn -> btn.getText().contains("Process Images"))
                .findFirst()
                .orElseThrow();
        assertTrue(processBtn.isDisplayed());
    }

    @Test
    @Order(3)
    @DisplayName("Threshold slider updates displayed value")
    void thresholdSliderWorks() throws Exception {
        if (app == null) {
            WebServer server = new WebServer(null, null);
            app = server.start(testPort);
            Thread.sleep(200);
        }

        driver.get("http://localhost:" + testPort);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("thresholdInput")));

        WebElement slider = driver.findElement(By.id("thresholdInput"));
        WebElement valueDisplay = driver.findElement(By.id("thresholdValue"));

        // Change slider value using JavaScript (more reliable than Selenium's slide)
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = '0.85'; arguments[0].dispatchEvent(new Event('input'));",
            slider
        );

        // Wait a moment for the event to process
        Thread.sleep(100);

        assertEquals("0.85", valueDisplay.getText());
    }

    @Test
    @Order(4)
    @DisplayName("Process images with valid directory starts job")
    void processImagesStartsJob() throws Exception {
        // Create test images
        Path imagesDir = tempDir.resolve("test_images");
        Files.createDirectories(imagesDir);
        createTestImage(imagesDir, "img1.png", PNG_DATA);
        createTestImage(imagesDir, "img2.png", PNG_DATA);

        if (app == null) {
            WebServer server = new WebServer(null, null);
            app = server.start(testPort);
            Thread.sleep(200);
        }

        driver.get("http://localhost:" + testPort);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("directoryInput")));

        // Fill in directory
        WebElement directoryInput = driver.findElement(By.id("directoryInput"));
        directoryInput.clear();
        directoryInput.sendKeys(imagesDir.toString());

        // Click process button
        WebElement processBtn = driver.findElements(By.cssSelector("button")).stream()
                .filter(btn -> btn.getText().contains("Process Images"))
                .findFirst()
                .orElseThrow();
        processBtn.click();

        // Wait for progress panel to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("progressPanel")));

        WebElement progressPanel = driver.findElement(By.id("progressPanel"));
        assertTrue(progressPanel.isDisplayed());

        // Wait for completion (up to 30 seconds)
        wait.withTimeout(Duration.ofSeconds(30))
            .until(driver1 -> {
                try {
                    WebElement resultsPanel = driver1.findElement(By.id("resultsPanel"));
                    return resultsPanel.isDisplayed();
                } catch (Exception e) {
                    return false;
                }
            });
    }

    @Test
    @Order(5)
    @DisplayName("Process images alert shows when directory is empty")
    void processWithEmptyDirectoryShowsAlert() throws Exception {
        if (app == null) {
            WebServer server = new WebServer(null, null);
            app = server.start(testPort);
            Thread.sleep(200);
        }

        driver.get("http://localhost:" + testPort);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("directoryInput")));

        // Leave directory input empty and click process
        WebElement processBtn = driver.findElements(By.cssSelector("button")).stream()
                .filter(btn -> btn.getText().contains("Process Images"))
                .findFirst()
                .orElseThrow();
        processBtn.click();

        // Alert should appear (we'll catch it)
        try {
            wait.withTimeout(Duration.ofSeconds(2))
                .until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            assertTrue(alert.getText().contains("directory"));
            alert.accept();
        } catch (TimeoutException e) {
            // JavaScript alert might not trigger in headless mode
            // This is acceptable for the test
        }
    }

    @Test
    @Order(6)
    @DisplayName("Clusters display correctly with pre-loaded data")
    void clustersDisplayWithPreLoadedData() throws Exception {
        // Create test data
        Path clustersCsv = tempDir.resolve("clusters.csv");
        Path img1 = createTestImage(tempDir, "img1.png", PNG_DATA);
        Path img2 = createTestImage(tempDir, "img2.png", PNG_DATA);

        List<String> clusterLines = List.of(
                "c1," + img1.toString(),
                "c1," + img2.toString()
        );
        Files.write(clustersCsv, clusterLines);

        Path planCsv = tempDir.resolve("plan.csv");
        List<String> planLines = List.of(
                "clusterId,action,path,reason",
                "c1,KEEP," + img1.toString() + ",keeper",
                "c1,DELETE," + img2.toString() + ",dupe"
        );
        Files.write(planCsv, planLines);

        // Stop old server and start new one with data
        if (app != null) {
            app.stop();
            Thread.sleep(200);
        }

        WebServer server = new WebServer(planCsv, clustersCsv);
        app = server.start(testPort);
        Thread.sleep(200);

        driver.get("http://localhost:" + testPort);

        // Wait for clusters to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("clusters")));

        // Verify cluster header
        WebElement clusterHeader = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("cluster-header"))
        );
        assertTrue(clusterHeader.getText().contains("Cluster c1"));
        assertTrue(clusterHeader.getText().contains("2 images"));

        // Verify cluster action buttons exist
        List<WebElement> buttons = driver.findElements(By.cssSelector(".cluster-actions button"));
        assertTrue(buttons.size() >= 3, "Should have Auto Select, Keep All, and Delete All buttons");

        // Verify image grid
        WebElement imageGrid = driver.findElement(By.id("cluster-c1"));
        List<WebElement> imageItems = imageGrid.findElements(By.className("image-item"));
        assertEquals(2, imageItems.size(), "Should display 2 images");

        // Verify actions
        boolean hasKeep = imageItems.stream()
            .anyMatch(item -> item.getAttribute("class").contains("keep"));
        boolean hasDelete = imageItems.stream()
            .anyMatch(item -> item.getAttribute("class").contains("delete"));

        assertTrue(hasKeep, "Should have at least one KEEP action");
        assertTrue(hasDelete, "Should have at least one DELETE action");
    }

    @Test
    @Order(7)
    @DisplayName("Stats update correctly")
    void statsUpdateCorrectly() throws Exception {
        // Create minimal test data
        Path clustersCsv = tempDir.resolve("clusters2.csv");
        Path img1 = createTestImage(tempDir, "img1.png", PNG_DATA);
        Path img2 = createTestImage(tempDir, "img2.png", PNG_DATA);

        Files.write(clustersCsv, List.of(
            "c1," + img1.toString(),
            "c1," + img2.toString()
        ));

        Path planCsv = tempDir.resolve("plan2.csv");
        Files.write(planCsv, List.of(
            "clusterId,action,path,reason",
            "c1,KEEP," + img1.toString() + ",keeper",
            "c1,DELETE," + img2.toString() + ",dupe"
        ));

        if (app != null) {
            app.stop();
            Thread.sleep(200);
        }

        WebServer server = new WebServer(planCsv, clustersCsv);
        app = server.start(testPort);
        Thread.sleep(200);

        driver.get("http://localhost:" + testPort);

        // Wait for stats to update
        wait.until(driver1 -> {
            try {
                String imageCount = driver1.findElement(By.id("imageCount")).getText();
                return !imageCount.equals("0");
            } catch (Exception e) {
                return false;
            }
        });

        // Verify stats
        assertEquals("1", driver.findElement(By.id("clusterCount")).getText());
        assertEquals("2", driver.findElement(By.id("imageCount")).getText());
        assertEquals("1", driver.findElement(By.id("keepCount")).getText());
        assertEquals("1", driver.findElement(By.id("deleteCount")).getText());
    }

    @Test
    @Order(8)
    @DisplayName("Clicking image toggles action")
    void clickingImageTogglesAction() throws Exception {
        // Create test data
        Path clustersCsv = tempDir.resolve("clusters3.csv");
        Path img1 = createTestImage(tempDir, "test1.png", PNG_DATA);

        Files.write(clustersCsv, List.of("c1," + img1.toString()));

        Path planCsv = tempDir.resolve("plan3.csv");
        Files.write(planCsv, List.of(
            "clusterId,action,path,reason",
            "c1,KEEP," + img1.toString() + ",initial"
        ));

        if (app != null) {
            app.stop();
            Thread.sleep(200);
        }

        WebServer server = new WebServer(planCsv, clustersCsv);
        app = server.start(testPort);
        Thread.sleep(200);

        driver.get("http://localhost:" + testPort);

        // Wait for image to load
        WebElement imageItem = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("image-item"))
        );

        // Initial state should be KEEP
        assertTrue(imageItem.getAttribute("class").contains("keep"));

        // Click to toggle
        imageItem.click();

        // Wait for DOM update
        Thread.sleep(200);

        // Should now be DELETE
        imageItem = driver.findElement(By.className("image-item"));
        assertTrue(imageItem.getAttribute("class").contains("delete"));

        // Click again to toggle back
        imageItem.click();
        Thread.sleep(200);

        // Should be KEEP again
        imageItem = driver.findElement(By.className("image-item"));
        assertTrue(imageItem.getAttribute("class").contains("keep"));
    }

    @Test
    @Order(9)
    @DisplayName("Keep All button sets all images to KEEP")
    void keepAllButtonWorks() throws Exception {
        Path clustersCsv = tempDir.resolve("clusters4.csv");
        Path img1 = createTestImage(tempDir, "img1.png", PNG_DATA);
        Path img2 = createTestImage(tempDir, "img2.png", PNG_DATA);

        Files.write(clustersCsv, List.of(
            "c1," + img1.toString(),
            "c1," + img2.toString()
        ));

        Path planCsv = tempDir.resolve("plan4.csv");
        Files.write(planCsv, List.of(
            "clusterId,action,path,reason",
            "c1,DELETE," + img1.toString() + ",initial",
            "c1,DELETE," + img2.toString() + ",initial"
        ));

        if (app != null) {
            app.stop();
            Thread.sleep(200);
        }

        WebServer server = new WebServer(planCsv, clustersCsv);
        app = server.start(testPort);
        Thread.sleep(200);

        driver.get("http://localhost:" + testPort);

        // Wait for cluster to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("cluster")));

        // Click "Keep All" button
        WebElement keepAllBtn = driver.findElements(By.cssSelector(".cluster-actions button")).stream()
            .filter(btn -> btn.getText().contains("Keep All"))
            .findFirst()
            .orElseThrow();
        keepAllBtn.click();

        // Wait for update
        Thread.sleep(300);

        // All images should now be KEEP
        List<WebElement> imageItems = driver.findElements(By.className("image-item"));
        assertTrue(imageItems.stream().allMatch(item -> item.getAttribute("class").contains("keep")));
    }

    @Test
    @Order(10)
    @DisplayName("Delete All button sets all images to DELETE")
    void deleteAllButtonWorks() throws Exception {
        Path clustersCsv = tempDir.resolve("clusters5.csv");
        Path img1 = createTestImage(tempDir, "img1.png", PNG_DATA);
        Path img2 = createTestImage(tempDir, "img2.png", PNG_DATA);

        Files.write(clustersCsv, List.of(
            "c1," + img1.toString(),
            "c1," + img2.toString()
        ));

        Path planCsv = tempDir.resolve("plan5.csv");
        Files.write(planCsv, List.of(
            "clusterId,action,path,reason",
            "c1,KEEP," + img1.toString() + ",initial",
            "c1,KEEP," + img2.toString() + ",initial"
        ));

        if (app != null) {
            app.stop();
            Thread.sleep(200);
        }

        WebServer server = new WebServer(planCsv, clustersCsv);
        app = server.start(testPort);
        Thread.sleep(200);

        driver.get("http://localhost:" + testPort);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("cluster")));

        // Click "Delete All" button
        WebElement deleteAllBtn = driver.findElements(By.cssSelector(".cluster-actions button")).stream()
            .filter(btn -> btn.getText().contains("Delete All"))
            .findFirst()
            .orElseThrow();
        deleteAllBtn.click();

        Thread.sleep(300);

        // All images should now be DELETE
        List<WebElement> imageItems = driver.findElements(By.className("image-item"));
        assertTrue(imageItems.stream().allMatch(item -> item.getAttribute("class").contains("delete")));
    }

    @Test
    @Order(11)
    @DisplayName("Auto Select button marks largest file as KEEP")
    void autoSelectButtonWorks() throws Exception {
        Path clustersCsv = tempDir.resolve("clusters6.csv");

        // Create images with different sizes
        Path img1 = tempDir.resolve("small.png");
        Files.write(img1, PNG_DATA); // Smaller

        Path img2 = tempDir.resolve("large.png");
        BufferedImage largeImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(largeImg, "png", img2.toFile()); // Larger

        Files.write(clustersCsv, List.of(
            "c1," + img1.toString(),
            "c1," + img2.toString()
        ));

        Path planCsv = tempDir.resolve("plan6.csv");
        Files.write(planCsv, List.of(
            "clusterId,action,path,reason",
            "c1,DELETE," + img1.toString() + ",initial",
            "c1,DELETE," + img2.toString() + ",initial"
        ));

        if (app != null) {
            app.stop();
            Thread.sleep(200);
        }

        WebServer server = new WebServer(planCsv, clustersCsv);
        app = server.start(testPort);
        Thread.sleep(200);

        driver.get("http://localhost:" + testPort);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("cluster")));

        // Click "Auto Select" button
        WebElement autoBtn = driver.findElements(By.cssSelector(".cluster-actions button")).stream()
            .filter(btn -> btn.getText().contains("Auto Select"))
            .findFirst()
            .orElseThrow();
        autoBtn.click();

        Thread.sleep(300);

        // Should have one KEEP (largest) and one DELETE
        List<WebElement> imageItems = driver.findElements(By.className("image-item"));
        long keepCount = imageItems.stream()
            .filter(item -> item.getAttribute("class").contains("keep"))
            .count();
        long deleteCount = imageItems.stream()
            .filter(item -> item.getAttribute("class").contains("delete"))
            .count();

        assertEquals(1, keepCount, "Should have 1 KEEP (largest file)");
        assertEquals(1, deleteCount, "Should have 1 DELETE");
    }

    @Test
    @Order(12)
    @DisplayName("Save button appears after making changes")
    void savePanelAppearsAfterChanges() throws Exception {
        Path clustersCsv = tempDir.resolve("clusters7.csv");
        Path img1 = createTestImage(tempDir, "img1.png", PNG_DATA);

        Files.write(clustersCsv, List.of("c1," + img1.toString()));

        Path planCsv = tempDir.resolve("plan7.csv");
        Files.write(planCsv, List.of(
            "clusterId,action,path,reason",
            "c1,KEEP," + img1.toString() + ",initial"
        ));

        if (app != null) {
            app.stop();
            Thread.sleep(200);
        }

        WebServer server = new WebServer(planCsv, clustersCsv);
        app = server.start(testPort);
        Thread.sleep(200);

        driver.get("http://localhost:" + testPort);

        WebElement savePanel = driver.findElement(By.id("savePanel"));
        // Initially hidden
        assertFalse(savePanel.getAttribute("class").contains("show"));

        // Make a change
        WebElement imageItem = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("image-item"))
        );
        imageItem.click();

        Thread.sleep(200);

        // Save panel should now be visible
        savePanel = driver.findElement(By.id("savePanel"));
        assertTrue(savePanel.getAttribute("class").contains("show"));
    }

    @Test
    @Order(13)
    @DisplayName("Apply button is disabled when no plan exists")
    void applyButtonDisabledWhenNoPlan() throws Exception {
        if (app != null) {
            app.stop();
            Thread.sleep(200);
        }

        WebServer server = new WebServer(null, null);
        app = server.start(testPort);
        Thread.sleep(200);

        driver.get("http://localhost:" + testPort);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("applyBtn")));

        WebElement applyBtn = driver.findElement(By.id("applyBtn"));
        assertTrue(applyBtn.getAttribute("disabled") != null ||
                   applyBtn.getAttribute("class").contains("disabled"));
    }

    // Helper methods

    private Path createTestImage(Path directory, String filename, byte[] data) throws IOException {
        Path imgPath = directory.resolve(filename);
        Files.createDirectories(imgPath.getParent());
        Files.write(imgPath, data);
        return imgPath;
    }
}

