package app;

import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

//import org.apache.commons.csv.*;


import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public class WebServer {

    private Path planCsv;
    private Path clustersCsv;
    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();
    private static class JobStatus {
        String status; // "running", "completed", "failed"
        int progress; // 0-100
        String message;
        String error;
        String result;
    }


    public WebServer(Path planCsv, Path clustersCsv) {
        this.planCsv = planCsv;
        this.clustersCsv = clustersCsv;
    }

    public Javalin start(int port) {
        var app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
        });

        app.post("/api/process", ctx -> {
            String directory = ctx.queryParam("directory");
            double threshold = Double.parseDouble(ctx.queryParamAsClass("threshold", String.class).getOrDefault("0.95"));
            String algo = ctx.queryParamAsClass("algo", String.class).getOrDefault("phash");

            if (directory == null || directory.isEmpty()) {
                ctx.status(400).result("Missing directory parameter");
                return;
            }

            String jobId = UUID.randomUUID().toString();
            JobStatus job = new JobStatus();
            job.status = "running";
            job.progress = 0;
            job.message = "Starting...";
            jobs.put(jobId, job);

            CompletableFuture.runAsync(() -> {
                try {
                    Path dirPath = Path.of(directory);
                    if (!Files.exists(dirPath)) {
                        job.status = "failed";
                        job.error = "Directory does not exist";
                        return;
                    }

                    // Step 1: Hash images
                    job.progress = 10;
                    job.message = "Hashing images with " + algo.toUpperCase() + "...";

                    Path hashFile = dirPath.resolve("hashes.csv");
                    Commands.hashImages(directory, hashFile.toString(), algo);

                    job.progress = 40;
                    job.message = "Finding duplicates...";

                    // Step 2: Cluster
                    Path clustersFile = dirPath.resolve("clusters.csv");
                    Commands.clusterImages(hashFile.toString(), clustersFile.toString(), threshold);

                    job.progress = 70;
                    job.message = "Generating plan...";

                    // Step 3: Generate plan
                    Path planFile = dirPath.resolve("plan.csv");
                    Commands.generatePlan(clustersFile.toString(), planFile.toString());

                    // Verify files exist and have content
                    if (!Files.exists(clustersFile) || Files.size(clustersFile) == 0) {
                        job.status = "failed";
                        job.error = "Clusters file is empty or missing";
                        return;
                    }

                    if (!Files.exists(planFile) || Files.size(planFile) == 0) {
                        job.status = "failed";
                        job.error = "Plan file is empty or missing";
                        return;
                    }

                    job.progress = 90;
                    job.message = "Finalizing...";

                    // Update server's file references ONLY after files are confirmed to exist
                    this.clustersCsv = clustersFile;
                    this.planCsv = planFile;

                    job.status = "completed";
                    job.progress = 100;
                    job.message = "Complete";
                    job.result = clustersFile.toString();

                } catch (Exception e) {
                    job.status = "failed";
                    job.error = e.getMessage();
                    e.printStackTrace();
                }
            });




            ctx.json(Map.of("jobId", jobId, "status", "started"));
        });

        app.get("/api/job/{jobId}", ctx -> {
            String jobId = ctx.pathParam("jobId");
            JobStatus job = jobs.get(jobId);

            if (job == null) {
                ctx.status(404).json(Map.of("error", "Job not found"));
                return;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", job.status);
            response.put("progress", job.progress);
            response.put("message", job.message);

            if (job.error != null) {
                response.put("error", job.error);
            }
            if (job.result != null) {
                response.put("result", job.result);
            }

            ctx.json(response);
        });


        app.get("/api/clusters", ctx -> {
            ctx.json(loadClusters());
        });

        app.get("/api/plan", ctx -> {
            ctx.json(loadPlan());
        });

        app.get("/api/image", ctx -> {
            String pathParam = ctx.queryParam("path");

            if (pathParam == null || pathParam.isEmpty()) {
                ctx.status(400).result("Missing path parameter");
                return;
            }

            try {
                Path imagePath = Path.of(pathParam);

                if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                    ctx.status(404).result("Image not found");
                    return;
                }

                // Determine content-type based on file extension
                String fileName = imagePath.getFileName().toString().toLowerCase();
                String contentType;
                if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (fileName.endsWith(".bmp")) {
                    contentType = "image/bmp";
                } else if (fileName.endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    contentType = "application/octet-stream";
                }

                ctx.contentType(contentType);
                ctx.result(Files.readAllBytes(imagePath));

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error loading image: " + e.getMessage());
            }
        });


        app.post("/api/plan/update", ctx -> {
            try {
                String body = ctx.body();
                System.out.println("Received body: " + body); // Debug log

                Gson gson = new Gson();
                Type type = new TypeToken<List<Map<String, String>>>(){}.getType();
                List<Map<String, String>> updates = gson.fromJson(ctx.body(), type);

                System.out.println("Parsed " + updates.size() + " updates"); // Debug log

                savePlan(updates);
                System.out.println("Saved to: " + planCsv); // Debug log

                ctx.result("OK");
            } catch (Exception e) {
                e.printStackTrace(); // Print full stack trace
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.post("/api/apply", ctx -> {
            if (this.planCsv == null || !Files.exists(this.planCsv)) {
                ctx.status(400).result("No plan available to apply");
                return;
            }

            String jobId = UUID.randomUUID().toString();
            JobStatus job = new JobStatus();
            job.status = "running";
            job.progress = 0;
            job.message = "Starting apply...";
            jobs.put(jobId, job);

            CompletableFuture.runAsync(() -> {
                try {
                    job.progress = 5;
                    job.message = "Preparing apply...";

                    Commands.Apply applyCmd = new Commands.Apply();
                    applyCmd.planCsv = this.planCsv;
                    applyCmd.quarantine = Path.of("./quarantine");
                    applyCmd.hardlink = false;

                    job.progress = 10;
                    job.message = "Applying plan...";

                    int result = applyCmd.call();

                    if (result == 0) {
                        job.progress = 100;
                        job.status = "completed";
                        job.message = "Apply completed";
                    } else {
                        job.status = "failed";
                        job.error = "Apply returned code: " + result;
                        job.message = "Apply failed";
                    }
                } catch (Exception e) {
                    job.status = "failed";
                    job.error = e.getMessage();
                    e.printStackTrace();
                }
            });

            ctx.json(Map.of("jobId", jobId, "status", "started"));
        });

        return app.start(port);
    }

    private List<Map<String, Object>> loadClusters() throws IOException {
        if (clustersCsv == null || !Files.exists(clustersCsv)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();

        for (String line : Files.readAllLines(clustersCsv)) {
            if (line.isBlank()) continue;
            int c = line.indexOf(',');
            String cid = line.substring(0, c);
            String path = line.substring(c + 1);

            // Get file metadata
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("path", path);

            try {
                Path filePath = Path.of(path);
                if (Files.exists(filePath)) {
                    fileInfo.put("size", Files.size(filePath));
                    fileInfo.put("modified", Files.getLastModifiedTime(filePath).toMillis());
                } else {
                    fileInfo.put("size", 0L);
                    fileInfo.put("modified", 0L);
                }
            } catch (Exception e) {
                fileInfo.put("size", 0L);
                fileInfo.put("modified", 0L);
            }

            groups.computeIfAbsent(cid, k -> new ArrayList<>()).add(fileInfo);
        }

        groups.forEach((cid, files) -> {
            result.add(Map.of("id", cid, "files", files));
        });

        return result;
    }


    private void savePlan(List<Map<String, String>> updates) throws IOException {
        try (var writer = Files.newBufferedWriter(planCsv);
             var csv = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            csv.printRecord("clusterId", "action", "path", "reason");

            for (Map<String, String> item : updates) {
                csv.printRecord(
                        item.getOrDefault("clusterId", ""),
                        item.getOrDefault("action", "keep"),
                        item.getOrDefault("path", ""),
                        item.getOrDefault("reason", "")
                );
            }
        }
    }

    private List<Map<String, String>> loadPlan() throws Exception {
        if (planCsv == null || !Files.exists(planCsv)) {
            return new ArrayList<>();
        }

        List<String> lines = Files.readAllLines(planCsv);
        if (lines.isEmpty() || lines.size() == 1) { // Only header or empty
            return new ArrayList<>();
        }

        List<Map<String, String>> result = new ArrayList<>();

        try (var reader = Files.newBufferedReader(planCsv);
             var csv = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csv) {
                Map<String, String> item = new HashMap<>();
                item.put("clusterId", record.get("clusterId"));
                item.put("action", record.get("action"));
                item.put("path", record.get("path"));
                item.put("reason", record.size() > 3 ? record.get("reason") : "");
                result.add(item);
            }
        }

        return result;
    }






}
