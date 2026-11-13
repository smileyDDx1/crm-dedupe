package com.debangshu.dedupe.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Union-find (disjoint set union) to turn matching PAIRS into GROUPS.
 *
 * If A matches B and B matches C, all three are one person, even though A and C were maybe never
 * compared directly. union(a, b) joins the two groups; find(x) returns the group's representative.
 * With union by size and path halving, each operation is almost O(1).
 */
public final class UnionFind {

    private final int[] parent;
    private final int[] size;

    public UnionFind(int elements) {
        parent = new int[elements];
        size = new int[elements];
        for (int i = 0; i < elements; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    public int find(int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]]; // path halving: point to the grandparent, the tree gets flatter
            x = parent[x];
        }
        return x;
    }

    public boolean union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) {
            return false;
        }
        // hang the smaller tree under the bigger one, so trees stay shallow
        if (size[rootA] < size[rootB]) {
            int swap = rootA;
            rootA = rootB;
            rootB = swap;
        }
        parent[rootB] = rootA;
        size[rootA] += size[rootB];
        return true;
    }

    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    /** Every group with more than one element, as lists of element indexes (in index order). */
    public List<List<Integer>> groups() {
        Map<Integer, List<Integer>> byRoot = new HashMap<>();
        for (int i = 0; i < parent.length; i++) {
            int root = find(i);
            if (size[root] > 1) {
                byRoot.computeIfAbsent(root, r -> new ArrayList<>()).add(i);
            }
        }
        List<List<Integer>> groups = new ArrayList<>(byRoot.values());
        groups.sort((g1, g2) -> Integer.compare(g1.getFirst(), g2.getFirst()));
        return groups;
    }
}
