package index;
import java.util.List;

public interface Index {
    void add(long hash, String id);
    List<String> withinHamming(long hash, int radius);
}
