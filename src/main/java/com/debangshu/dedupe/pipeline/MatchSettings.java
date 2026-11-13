package com.debangshu.dedupe.pipeline;

/**
 * @param matchThreshold  pairs scoring at least this are treated as the same person and merged
 * @param reviewThreshold pairs between this and matchThreshold are "maybe": written to review.csv for a human
 * @param maxBlockSize    see Blocker
 * @param exhaustive      compare ALL pairs instead of blocking (slow, only for measuring what blocking misses)
 * @param parallel        score pairs on all CPU cores
 */
public record MatchSettings(double matchThreshold, double reviewThreshold, int maxBlockSize,
                            boolean exhaustive, boolean parallel) {

    public static final double DEFAULT_MATCH_THRESHOLD = 0.85;
    public static final double DEFAULT_REVIEW_THRESHOLD = 0.70;
    public static final int DEFAULT_MAX_BLOCK_SIZE = 1000;

    public MatchSettings {
        if (matchThreshold <= 0 || matchThreshold > 1 || reviewThreshold <= 0 || reviewThreshold > 1) {
            throw new IllegalArgumentException("thresholds must be between 0 and 1");
        }
        if (reviewThreshold > matchThreshold) {
            throw new IllegalArgumentException("the review threshold can't be higher than the match threshold");
        }
        if (maxBlockSize < 2) {
            throw new IllegalArgumentException("max block size must be at least 2");
        }
    }

    public static MatchSettings defaults() {
        return new MatchSettings(DEFAULT_MATCH_THRESHOLD, DEFAULT_REVIEW_THRESHOLD, DEFAULT_MAX_BLOCK_SIZE, false, true);
    }
}
