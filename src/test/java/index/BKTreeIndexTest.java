package index;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BKTreeIndexTest {

    private BKTreeIndex index;

    @BeforeEach
    void setUp() {
        index = new BKTreeIndex();
    }

    @Test
    void testWithinHamming_EmptyTree() {
        List<String> results = index.withinHamming(12345L, 2);
        assertTrue(results.isEmpty(), "Searching in an empty tree should yield no results.");
    }

    @Test
    void testAdd_SingleItem_AndSearchExact() {
        index.add(0b1100, "id1");
        List<String> results = index.withinHamming(0b1100, 0);
        assertEquals(1, results.size());
        assertTrue(results.contains("id1"));
    }

    @Test
    void testAdd_SingleItem_AndSearchWithinRadius() {
        index.add(0b1100, "id1");
        // Hamming distance between 0b1100 and 0b1101 is 1
        List<String> results = index.withinHamming(0b1101, 1);
        assertEquals(1, results.size());
        assertTrue(results.contains("id1"));
    }

    @Test
    void testAdd_SingleItem_AndSearchOutsideRadius() {
        index.add(0b1100, "id1");
        // Hamming distance between 0b1100 and 0b0011 is 4
        List<String> results = index.withinHamming(0b0011, 2);
        assertTrue(results.isEmpty());
    }

    @Test
    void testAdd_DuplicateHashesWithDifferentIds() {
        index.add(0b101010, "id1");
        index.add(0b101010, "id2");

        List<String> results = index.withinHamming(0b101010, 0);
        assertEquals(2, results.size());
        assertTrue(results.contains("id1"));
        assertTrue(results.contains("id2"));
    }

    @Test
    void testAdd_MultipleItems_AndSearch() {
        // Reference hash for searching
        long searchHash = 0b11110000; // 240

        // hash, id, distance from searchHash
        index.add(0b11110000, "id1"); // dist 0
        index.add(0b11110001, "id2"); // dist 1
        index.add(0b11110011, "id3"); // dist 2
        index.add(0b11111111, "id4"); // dist 4
        index.add(0b00000000, "id5"); // dist 4

        // Search with radius 0
        List<String> r0 = index.withinHamming(searchHash, 0);
        assertEquals(1, r0.size());
        assertTrue(r0.contains("id1"));

        // Search with radius 1
        List<String> r1 = index.withinHamming(searchHash, 1);
        assertEquals(2, r1.size());
        assertTrue(r1.containsAll(List.of("id1", "id2")));

        // Search with radius 2
        List<String> r2 = index.withinHamming(searchHash, 2);
        assertEquals(3, r2.size());
        assertTrue(r2.containsAll(List.of("id1", "id2", "id3")));

        // Search with radius 3 (should be same as radius 2)
        List<String> r3 = index.withinHamming(searchHash, 3);
        assertEquals(3, r3.size());
        assertTrue(r3.containsAll(List.of("id1", "id2", "id3")));

        // Search with radius 4
        List<String> r4 = index.withinHamming(searchHash, 4);
        assertEquals(5, r4.size());
        assertTrue(r4.containsAll(List.of("id1", "id2", "id3", "id4", "id5")));
    }

    @Test
    void testWithinHamming_NoMatches() {
        index.add(1L, "id1");
        index.add(10L, "id2");
        index.add(100L, "id3");

        List<String> results = index.withinHamming(1000L, 1);
        assertTrue(results.isEmpty());
    }
}