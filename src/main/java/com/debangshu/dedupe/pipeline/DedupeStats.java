package com.debangshu.dedupe.pipeline;

/**
 * Counts and timings of one run, printed at the end and used by the evaluate command.
 * All durations are in milliseconds.
 */
public record DedupeStats(int rows, long allPairs, long comparedPairs, int blocks, int skippedBlocks,
                          int matchPairs, int reviewPairs, int clusters, int duplicateRows,
                          long normalizeMs, long blockingMs, long scoringMs, long clusteringMs) {

    /** How many comparisons blocking saved, in percent of all possible pairs. */
    public double comparisonsSavedPercent() {
        return allPairs == 0 ? 0 : 100.0 * (allPairs - comparedPairs) / allPairs;
    }

    public long totalMs() {
        return normalizeMs + blockingMs + scoringMs + clusteringMs;
    }
}
