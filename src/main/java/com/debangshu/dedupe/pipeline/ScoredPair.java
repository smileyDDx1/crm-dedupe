package com.debangshu.dedupe.pipeline;

/** Two rows (by index in the input list) and how likely they are the same person. */
public record ScoredPair(int first, int second, double score) {
}
