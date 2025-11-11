package cluster;

import index.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Clusterer Tests")
class ClustererTest {

    private Index mockIndex;

    @BeforeEach
    void setUp() {
        // Create a mock Index object before each test
        mockIndex = mock(Index.class);
    }

    @Test
    @DisplayName("Should return an empty list when given no items")
    void cluster_withEmptyItems_shouldReturnEmptyList() {
        // Given: An empty map of items
        Map<String, Long> items = Map.of();

        // When: The cluster method is called
        List<Cluster> clusters = Clusterer.cluster(items, mockIndex, 2);

        // Then: The result is an empty list and the index is not used
        assertTrue(clusters.isEmpty(), "The list of clusters should be empty.");
        verifyNoInteractions(mockIndex);
    }

    @Test
    @DisplayName("Should create a single cluster for each item when no items are related")
    void cluster_withNoRelatedItems_shouldCreateSeparateClusters() {
        // Given: A map of items where no two items are within the given radius
        Map<String, Long> items = Map.of(
                "item1", 1L,
                "item2", 2L,
                "item3", 3L
        );

        // Mock the index to return only the item itself for any query
        when(mockIndex.withinHamming(anyLong(), anyInt())).thenAnswer(invocation -> {
            long hash = invocation.getArgument(0);
            String key = "item" + hash;
            return List.of(key);
        });

        // When: The cluster method is called
        List<Cluster> clusters = Clusterer.cluster(items, mockIndex, 1);

        // Then: There should be a separate cluster for each item
        assertEquals(3, clusters.size(), "There should be 3 clusters.");
        Set<String> allClusteredItems = clusters.stream()
                .flatMap(c -> c.members().stream())
                .collect(Collectors.toSet());

        assertEquals(Set.of("item1", "item2", "item3"), allClusteredItems);
        clusters.forEach(cluster -> assertEquals(1, cluster.members().size(), "Each cluster should have only one member."));
    }

    @Test
    @DisplayName("Should group all items into a single cluster when all are related")
    void cluster_withAllItemsRelated_shouldCreateOneCluster() {
        // Given: A map of items that are all interconnected
        Map<String, Long> items = Map.of(
                "item1", 10L,
                "item2", 20L,
                "item3", 30L
        );

        // Mock the index to create a chain of neighbors
        when(mockIndex.withinHamming(10L, 2)).thenReturn(List.of("item1", "item2"));
        when(mockIndex.withinHamming(20L, 2)).thenReturn(List.of("item1", "item2", "item3"));
        when(mockIndex.withinHamming(30L, 2)).thenReturn(List.of("item2", "item3"));

        // When: The cluster method is called
        List<Cluster> clusters = Clusterer.cluster(items, mockIndex, 2);

        // Then: All items should be in a single cluster
        assertEquals(1, clusters.size(), "There should be only one cluster.");
        Cluster singleCluster = clusters.get(0);
        assertEquals(3, singleCluster.members().size(), "The cluster should contain 3 members.");
        assertTrue(singleCluster.members().containsAll(List.of("item1", "item2", "item3")),
                "The cluster should contain all items.");
    }

    @Test
    @DisplayName("Should create multiple distinct clusters for different groups of items")
    void cluster_withMultipleDistinctGroups_shouldCreateMultipleClusters() {
        // Given: A map of items that form two distinct groups
        Map<String, Long> items = Map.of(
                "A1", 1L, "A2", 2L, // Group A
                "B1", 101L, "B2", 102L, // Group B
                "C1", 201L // Group C (single item)
        );

        // Mock the index for Group A
        when(mockIndex.withinHamming(1L, 3)).thenReturn(List.of("A1", "A2"));
        when(mockIndex.withinHamming(2L, 3)).thenReturn(List.of("A1", "A2"));

        // Mock the index for Group B
        when(mockIndex.withinHamming(101L, 3)).thenReturn(List.of("B1", "B2"));
        when(mockIndex.withinHamming(102L, 3)).thenReturn(List.of("B1", "B2"));

        // Mock the index for Group C
        when(mockIndex.withinHamming(201L, 3)).thenReturn(List.of("C1"));

        // When: The cluster method is called
        List<Cluster> clusters = Clusterer.cluster(items, mockIndex, 3);

        // Then: Three distinct clusters should be formed
        assertEquals(3, clusters.size(), "There should be 3 distinct clusters.");

        // Extract members of each cluster for easier assertion
        Set<Set<String>> clusterMemberSets = clusters.stream()
                .map(c -> Set.copyOf(c.members()))
                .collect(Collectors.toSet());

        assertTrue(clusterMemberSets.contains(Set.of("A1", "A2")), "A cluster for Group A should exist.");
        assertTrue(clusterMemberSets.contains(Set.of("B1", "B2")), "A cluster for Group B should exist.");
        assertTrue(clusterMemberSets.contains(Set.of("C1")), "A cluster for Group C should exist.");
    }

    @Test
    @DisplayName("Should not re-cluster an item that has already been visited")
    void cluster_shouldNotRevisitItems() {
        // Given: Two items that are neighbors
        Map<String, Long> items = Map.of("item1", 1L, "item2", 2L);

        when(mockIndex.withinHamming(1L, 1)).thenReturn(List.of("item1", "item2"));
        when(mockIndex.withinHamming(2L, 1)).thenReturn(List.of("item1", "item2"));

        // When: The cluster method is called
        List<Cluster> clusters = Clusterer.cluster(items, mockIndex, 1);

        // Then: One cluster is formed
        assertEquals(1, clusters.size());
        assertEquals(2, clusters.get(0).members().size());

        // And: The index is queried for each item only once during the traversal
        verify(mockIndex, times(1)).withinHamming(1L, 1);
        verify(mockIndex, times(1)).withinHamming(2L, 1);
    }
}