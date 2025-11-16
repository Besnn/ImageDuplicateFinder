package app;

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


public class WebServer {

    private final Path planCsv;
    private final Path clustersCsv;

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

        // API endpoints
        app.get("/api/clusters", ctx -> {
            ctx.json(loadClusters());
        });

        app.get("/api/plan", ctx -> {
            ctx.json(loadPlan());
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





        return app.start(port);
    }

        private List<Map<String, Object>> loadClusters() throws IOException {
        if (clustersCsv == null || !Files.exists(clustersCsv)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, List<String>> groups = new LinkedHashMap<>();

        for (String line : Files.readAllLines(clustersCsv)) {
            if (line.isBlank()) continue;
            int c = line.indexOf(',');
            String cid = line.substring(0, c);
            String path = line.substring(c + 1);
            groups.computeIfAbsent(cid, k -> new ArrayList<>()).add(path);
        }

        groups.forEach((cid, paths) -> {
            result.add(Map.of("id", cid, "paths", paths));
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

        List<Map<String, String>> result = new ArrayList<>();

        try (var reader = Files.newBufferedReader(planCsv);
             var csv = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csv) {
                Map<String, String> item = new HashMap<>();
                item.put("clusterId", record.get(0));
                item.put("action", record.get(1));
                item.put("path", record.get(2));
                item.put("reason", record.size() > 3 ? record.get(3) : "");
                result.add(item);
            }
        }

        return result;
    }





}
