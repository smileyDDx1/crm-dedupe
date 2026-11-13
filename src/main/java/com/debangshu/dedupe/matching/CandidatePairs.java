package com.debangshu.dedupe.matching;

import java.util.Arrays;

/**
 * A set of (i, j) row pairs, stored as one long each: i in the upper 32 bits, j in the lower 32 bits.
 *
 * Why not a HashSet<Pair>? There can be millions of pairs. A HashSet would allocate an object plus a
 * hash table entry for every one of them (40+ bytes each); a long[] is 8 bytes per pair. Duplicates
 * (two rows that share several blocking keys) are removed at the end by sorting, O(n log n).
 */
public final class CandidatePairs {

    private long[] pairs = new long[1024];
    private int size;

    public void add(int a, int b) {
        int low = Math.min(a, b);
        int high = Math.max(a, b);
        if (size == pairs.length) {
            pairs = Arrays.copyOf(pairs, pairs.length * 2);
        }
        pairs[size++] = encode(low, high);
    }

    /** Sorted, without duplicates. */
    public long[] toSortedUniqueArray() {
        long[] sorted = Arrays.copyOf(pairs, size);
        Arrays.sort(sorted);
        int unique = 0;
        for (int i = 0; i < sorted.length; i++) {
            if (i == 0 || sorted[i] != sorted[i - 1]) {
                sorted[unique++] = sorted[i];
            }
        }
        return Arrays.copyOf(sorted, unique);
    }

    public static long encode(int low, int high) {
        return ((long) low << 32) | (high & 0xffffffffL);
    }

    public static int first(long pair) {
        return (int) (pair >>> 32);
    }

    public static int second(long pair) {
        return (int) pair;
    }
}
