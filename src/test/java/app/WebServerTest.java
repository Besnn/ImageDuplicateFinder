package app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WebServerTest {

    @TempDir
    Path tempDir;

    private WebServer server;
    private Javalin app;
    private int testPort;
    private HttpClient client;
    private Gson gson;

    // Simple 1x1 black pixel PNG for testing
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

    @BeforeEach
    void setUp() {
        // Use a random available port for each test
        testPort = 17070 + (int) (Math.random() * 1000);
        client = HttpClient.newHttpClient();
        gson = new Gson();

        // Create server without pre-loaded files
        server = new WebServer(null, null);
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
            app = null;
        }
    }

    @Test
    @DisplayName("Server starts successfully on specified port")
    void serverStartsSuccessfully() throws Exception {
        app = server.start(testPort);
        assertNotNull(app, "Server should start and return Javalin instance");

        // Give server time to start
        TimeUnit.MILLISECONDS.sleep(100);

        // Try to access a basic endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Server should respond with 200 OK");
    }

    @Test
    @DisplayName("POST /api/process returns jobId when directory exists")
    void processEndpoint_validDirectory_returnsJobId() throws Exception {
        // Create test images directory
        Path imagesDir = tempDir.resolve("images");
        Files.createDirectories(imagesDir);
        createTestImage(imagesDir, "test1.png", PNG_DATA);
        createTestImage(imagesDir, "test2.png", PNG_DATA);

        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/process?directory=%s&threshold=0.95&algo=phash",
                testPort, encode(imagesDir.toString()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Should return 200 OK");

        Map<String, Object> result = gson.fromJson(response.body(), Map.class);
        assertNotNull(result.get("jobId"), "Response should contain jobId");
        assertEquals("started", result.get("status"), "Status should be 'started'");
    }

    @Test
    @DisplayName("POST /api/process returns 400 when directory parameter is missing")
    void processEndpoint_missingDirectory_returns400() throws Exception {
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/process", testPort);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode(), "Should return 400 Bad Request");
        assertTrue(response.body().contains("Missing directory parameter"));
    }

    @Test
    @DisplayName("POST /api/process creates dot-prefixed temp directory")
    void processEndpoint_createsDotPrefixedTempDir() throws Exception {
        Path imagesDir = tempDir.resolve("images");
        Files.createDirectories(imagesDir);
        createTestImage(imagesDir, "test1.png", PNG_DATA);
        createTestImage(imagesDir, "test2.png", PNG_DATA);

        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/process?directory=%s&threshold=0.95&algo=phash",
                testPort, encode(imagesDir.toString()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Map<String, Object> result = gson.fromJson(response.body(), Map.class);
        String jobId = (String) result.get("jobId");
        assertNotNull(jobId);

        // Wait for job to complete
        waitForJobCompletion(jobId, 10);

        // Check that a dot-prefixed directory was created
        boolean foundDotDir = Files.list(imagesDir)
                .anyMatch(p -> p.getFileName().toString().startsWith(".idf"));

        assertTrue(foundDotDir, "Should create a dot-prefixed temporary directory");
    }

    @Test
    @DisplayName("GET /api/job/{jobId} returns job status")
    void jobStatusEndpoint_returnsStatus() throws Exception {
        Path imagesDir = tempDir.resolve("images");
        Files.createDirectories(imagesDir);
        createTestImage(imagesDir, "test.png", PNG_DATA);

        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        // Start a processing job
        String processUrl = String.format("http://localhost:%d/api/process?directory=%s", testPort, encode(imagesDir.toString()));
        HttpRequest processRequest = HttpRequest.newBuilder()
                .uri(URI.create(processUrl))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> processResponse = client.send(processRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> processResult = gson.fromJson(processResponse.body(), Map.class);
        String jobId = (String) processResult.get("jobId");

        // Check job status
        String jobUrl = String.format("http://localhost:%d/api/job/%s", testPort, jobId);
        HttpRequest jobRequest = HttpRequest.newBuilder()
                .uri(URI.create(jobUrl))
                .GET()
                .build();

        HttpResponse<String> jobResponse = client.send(jobRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, jobResponse.statusCode());

        Map<String, Object> jobStatus = gson.fromJson(jobResponse.body(), Map.class);
        assertNotNull(jobStatus.get("status"), "Job status should contain status field");
        assertNotNull(jobStatus.get("progress"), "Job status should contain progress field");
    }

    @Test
    @DisplayName("GET /api/job/{jobId} returns 404 for non-existent job")
    void jobStatusEndpoint_nonExistentJob_returns404() throws Exception {
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String jobUrl = String.format("http://localhost:%d/api/job/non-existent-job-id", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jobUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Job not found"));
    }

    @Test
    @DisplayName("GET /api/clusters returns empty list when no clusters loaded")
    void clustersEndpoint_noClusters_returnsEmptyList() throws Exception {
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/clusters", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> clusters = gson.fromJson(response.body(), listType);
        assertEquals(0, clusters.size(), "Should return empty list when no clusters");
    }

    @Test
    @DisplayName("GET /api/clusters returns loaded clusters")
    void clustersEndpoint_withClusters_returnsClusters() throws Exception {
        // Create a clusters.csv file
        Path clustersCsv = tempDir.resolve("clusters.csv");
        Path img1 = createTestImage(tempDir, "img1.png", PNG_DATA);
        Path img2 = createTestImage(tempDir, "img2.png", PNG_DATA);

        List<String> clusterLines = List.of(
                "cluster1," + img1.toString(),
                "cluster1," + img2.toString()
        );
        Files.write(clustersCsv, clusterLines);

        // Create server with pre-loaded clusters
        server = new WebServer(null, clustersCsv);
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/clusters", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> clusters = gson.fromJson(response.body(), listType);

        assertEquals(1, clusters.size(), "Should return 1 cluster");
        assertEquals("cluster1", clusters.get(0).get("id"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) clusters.get(0).get("files");
        assertEquals(2, files.size(), "Cluster should contain 2 files");
    }

    @Test
    @DisplayName("GET /api/plan returns empty list when no plan loaded")
    void planEndpoint_noPlan_returnsEmptyList() throws Exception {
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/plan", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
        List<Map<String, String>> plan = gson.fromJson(response.body(), listType);
        assertEquals(0, plan.size(), "Should return empty list when no plan");
    }

    @Test
    @DisplayName("GET /api/plan returns loaded plan")
    void planEndpoint_withPlan_returnsPlan() throws Exception {
        Path planCsv = tempDir.resolve("plan.csv");
        Path img1 = createTestImage(tempDir, "keeper.png", PNG_DATA);
        Path img2 = createTestImage(tempDir, "dupe.png", PNG_DATA);

        List<String> planLines = List.of(
                "clusterId,action,path,reason",
                "c1,KEEP," + img1.toString() + ",keeper",
                "c1,DELETE," + img2.toString() + ",dupe"
        );
        Files.write(planCsv, planLines);

        server = new WebServer(planCsv, null);
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/plan", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
        List<Map<String, String>> plan = gson.fromJson(response.body(), listType);

        assertEquals(2, plan.size(), "Should return 2 plan entries");
        assertEquals("keep", plan.get(0).get("action"));
        assertEquals("delete", plan.get(1).get("action"));
    }

    @Test
    @DisplayName("POST /api/plan/update updates plan file")
    void planUpdateEndpoint_updatesPlan() throws Exception {
        Path planCsv = tempDir.resolve("plan.csv");
        Path img1 = createTestImage(tempDir, "img1.png", PNG_DATA);

        // Create initial plan
        List<String> planLines = List.of(
                "clusterId,action,path,reason",
                "c1,KEEP," + img1.toString() + ",initial"
        );
        Files.write(planCsv, planLines);

        server = new WebServer(planCsv, null);
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        // Update the plan
        List<Map<String, String>> updates = List.of(
                Map.of("clusterId", "c1", "action", "DELETE", "path", img1.toString(), "reason", "updated")
        );
        String json = gson.toJson(updates);

        String url = String.format("http://localhost:%d/api/plan/update", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        // Verify file was updated
        List<String> updatedLines = Files.readAllLines(planCsv);
        assertTrue(updatedLines.stream().anyMatch(line -> line.contains("delete")),
                "Plan file should contain updated action");
    }

    @Test
    @DisplayName("GET /api/image returns image file")
    void imageEndpoint_existingImage_returnsImage() throws Exception {
        Path imgPath = createTestImage(tempDir, "test.png", PNG_DATA);

        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String encodedPath = URLEncoder.encode(imgPath.toString(), StandardCharsets.UTF_8);
        String url = String.format("http://localhost:%d/api/image?path=%s", testPort, encodedPath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertEquals("image/png", response.headers().firstValue("content-type").orElse(""));
        assertArrayEquals(PNG_DATA, response.body(), "Should return image bytes");
    }

    @Test
    @DisplayName("GET /api/image returns 404 for non-existent image")
    void imageEndpoint_nonExistentImage_returns404() throws Exception {
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String encodedPath = URLEncoder.encode("/non/existent/image.png", StandardCharsets.UTF_8);
        String url = String.format("http://localhost:%d/api/image?path=%s", testPort, encodedPath);        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Image not found"));
    }

    @Test
    @DisplayName("GET /api/image returns 400 when path parameter missing")
    void imageEndpoint_missingPath_returns400() throws Exception {
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/image", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Missing path parameter"));
    }

    @Test
    @DisplayName("POST /api/apply returns 400 when no plan loaded")
    void applyEndpoint_noPlan_returns400() throws Exception {
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/apply", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("No plan available"));
    }

    @Test
    @DisplayName("POST /api/apply starts apply job")
    void applyEndpoint_withPlan_startsJob() throws Exception {
        Path planCsv = tempDir.resolve("plan.csv");
        Path img1 = createTestImage(tempDir, "keeper.png", PNG_DATA);
        Path img2 = createTestImage(tempDir, "dupe.png", PNG_DATA);

        List<String> planLines = List.of(
                "clusterId,action,path,reason",
                "c1,KEEP," + img1.toString() + ",keeper",
                "c1,DELETE," + img2.toString() + ",dupe"
        );
        Files.write(planCsv, planLines);

        server = new WebServer(planCsv, null);
        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        String url = String.format("http://localhost:%d/api/apply", testPort);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Map<String, Object> result = gson.fromJson(response.body(), Map.class);
        assertNotNull(result.get("jobId"), "Should return jobId");
        assertEquals("started", result.get("status"));
    }

    @Test
    @DisplayName("Complete workflow: process, check status, get clusters and plan")
    void completeWorkflow_processAndRetrieve() throws Exception {
        Path imagesDir = tempDir.resolve("images");
        Files.createDirectories(imagesDir);

        // Create identical images (will form a cluster)
        createTestImage(imagesDir, "img1.png", PNG_DATA);
        createTestImage(imagesDir, "img2.png", PNG_DATA);
        // Different image
        createTestImage(imagesDir, "img3.png", PNG_DATA_WHITE);

        app = server.start(testPort);
        TimeUnit.MILLISECONDS.sleep(100);

        // 1. Start processing
        String processUrl = String.format("http://localhost:%d/api/process?directory=%s&threshold=0.95&algo=phash",
                testPort, encode(imagesDir.toString()));
        HttpRequest processRequest = HttpRequest.newBuilder()
                .uri(URI.create(processUrl))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> processResponse = client.send(processRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, processResponse.statusCode());

        Map<String, Object> processResult = gson.fromJson(processResponse.body(), Map.class);
        String jobId = (String) processResult.get("jobId");

        // 2. Wait for job completion
        boolean completed = waitForJobCompletion(jobId, 15);
        assertTrue(completed, "Job should complete within timeout");

        // 3. Get clusters
        String clustersUrl = String.format("http://localhost:%d/api/clusters", testPort);
        HttpRequest clustersRequest = HttpRequest.newBuilder()
                .uri(URI.create(clustersUrl))
                .GET()
                .build();

        HttpResponse<String> clustersResponse = client.send(clustersRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, clustersResponse.statusCode());

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> clusters = gson.fromJson(clustersResponse.body(), listType);
        assertTrue(clusters.size() >= 1, "Should have at least one cluster with duplicates");

        // 4. Get plan
        String planUrl = String.format("http://localhost:%d/api/plan", testPort);
        HttpRequest planRequest = HttpRequest.newBuilder()
                .uri(URI.create(planUrl))
                .GET()
                .build();

        HttpResponse<String> planResponse = client.send(planRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, planResponse.statusCode());

        Type planType = new TypeToken<List<Map<String, String>>>(){}.getType();
        List<Map<String, String>> plan = gson.fromJson(planResponse.body(), planType);
        assertTrue(plan.size() >= 2, "Plan should have at least 2 entries (KEEP and DELETE)");

        // Verify plan has both KEEP and DELETE actions
        boolean hasKeep = plan.stream().anyMatch(entry -> "keep".equals(entry.get("action")));
        boolean hasDelete = plan.stream().anyMatch(entry -> "delete".equals(entry.get("action")));
        assertTrue(hasKeep, "Plan should have at least one KEEP action");
        assertTrue(hasDelete, "Plan should have at least one DELETE action");
    }

    // Helper methods

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private Path createTestImage(Path directory, String filename, byte[] data) throws IOException {
        Path imgPath = directory.resolve(filename);
        Files.createDirectories(imgPath.getParent());
        Files.write(imgPath, data);
        return imgPath;
    }

    private boolean waitForJobCompletion(String jobId, int timeoutSeconds) throws Exception {
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeout) {
            String jobUrl = String.format("http://localhost:%d/api/job/%s", testPort, jobId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jobUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<String, Object> jobStatus = gson.fromJson(response.body(), Map.class);
                String status = (String) jobStatus.get("status");

                if ("completed".equals(status)) {
                    return true;
                } else if ("failed".equals(status)) {
                    System.err.println("Job failed: " + jobStatus.get("error"));
                    return false;
                }
            }

            TimeUnit.MILLISECONDS.sleep(500);
        }

        return false;
    }
}

