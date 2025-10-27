package cluster;
import index.Index;
import java.util.*;

public final class Clusterer {
    public static List<Cluster> cluster(Map<String,Long> items, Index index, int radius) {
        Set<String> visited = new HashSet<>();
        List<Cluster> clusters = new ArrayList<>();
        for (var e : items.entrySet()) {
            String id = e.getKey();
            if (visited.contains(id)) continue;
            List<String> members = new ArrayList<>();
            Queue<String> q = new ArrayDeque<>();
            q.add(id);
            visited.add(id);
            while (!q.isEmpty()) {
                String cur = q.poll();
                members.add(cur);
                long h = items.get(cur);
                for (String nb : index.withinHamming(h, radius)) {
                    if (!visited.contains(nb)) {
                        visited.add(nb);
                        q.add(nb);
                    }
                }
            }
            clusters.add(new Cluster(UUID.randomUUID().toString(), members));
        }
        return clusters;
    }
}
