package index;

import java.util.*;

public class BKTreeIndex implements Index {
    private static class Node {
        long key;
        List<String> ids = new ArrayList<>();
        Map<Integer, Node> edges = new HashMap<>();
        Node(long k, String id) { key=k; ids.add(id); }
    }
    private Node root;

    @Override
    public void add(long hash, String id) {
        if (root == null) { root = new Node(hash, id); return; }
        Node cur = root;
        while (true) {
            int d = index.Hamming.distance(hash, cur.key);
            if (d == 0) { cur.ids.add(id); return; }
            Node next = cur.edges.get(d);
            if (next == null) { cur.edges.put(d, new Node(hash, id)); return; }
            cur = next;
        }
    }

    @Override
    public List<String> withinHamming(long hash, int radius) {
        List<String> out = new ArrayList<>();
        if (root == null) return out;
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Node n = stack.pop();
            int dist = index.Hamming.distance(hash, n.key);
            if (dist <= radius) out.addAll(n.ids);
            for (int k = dist - radius; k <= dist + radius; k++) {
                Node child = n.edges.get(k);
                if (child != null) stack.push(child);
            }
        }
        return out;
    }
}
