package app;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
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
                List<Map<String, String>> updates = ctx.bodyAsClass(List.class);
                savePlan(updates);
                ctx.result("OK");
            } catch (Exception e) {
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

    private List<Map<String, String>> loadPlan() throws Exception {
        if (planCsv == null || !Files.exists(planCsv)) {
            return List.of();
        }

        List<Map<String, String>> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(planCsv);

        for (int i = 1; i < lines.size(); i++) { // Skip header
            String line = lines.get(i);
            if (line.isBlank()) continue;
            String[] parts = line.split(",", 4);
            if (parts.length < 3) continue;
            result.add(Map.of(
                    "clusterId", parts[0],
                    "action", parts[1],
                    "path", parts[2],
                    "reason", parts.length > 3 ? parts[3] : ""
            ));
        }

        return result;
    }

    private void savePlan(List<Map<String, String>> updates) throws IOException {
        if (planCsv == null) {
            throw new IOException("No plan file configured");
        }

        List<String> lines = new ArrayList<>();
        lines.add("clusterId,action,path,reason");

        for (Map<String, String> item : updates) {
            String line = String.format("%s,%s,%s,%s",
                    item.getOrDefault("clusterId", ""),
                    item.getOrDefault("action", "keep"),
                    item.getOrDefault("path", ""),
                    item.getOrDefault("reason", "")
            );
            lines.add(line);
        }

        Files.write(planCsv, lines);
    }

}
