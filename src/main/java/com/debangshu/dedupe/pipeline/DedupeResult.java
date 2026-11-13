package com.debangshu.dedupe.pipeline;

import com.debangshu.dedupe.Contact;

import java.util.List;

/**
 * @param contacts       the input rows, in input order (ScoredPair indexes point into this list)
 * @param clusters       groups of 2+ rows that were merged
 * @param reviewPairs    "maybe" pairs that were NOT merged, best first
 * @param scoredPairs    every pair that scored at least the review threshold (used by the evaluator)
 * @param candidatePairs the pairs blocking produced, or null in exhaustive mode (used by the evaluator)
 */
public record DedupeResult(List<Contact> contacts, List<Cluster> clusters, List<ScoredPair> reviewPairs,
                           List<ScoredPair> scoredPairs, long[] candidatePairs, DedupeStats stats) {
}
