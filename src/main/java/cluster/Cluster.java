package cluster;
import java.util.List;

public record Cluster(String id, List<String> members) {}
