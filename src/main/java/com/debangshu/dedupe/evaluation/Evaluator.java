package com.debangshu.dedupe.evaluation;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.DedupeInputException;
import com.debangshu.dedupe.matching.CandidatePairs;
import com.debangshu.dedupe.matching.UnionFind;
import com.debangshu.dedupe.pipeline.ScoredPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Measures how good the matching is, on data where the right answer is known (true_entity_id).
 *
 * It counts PAIRS of rows:
 *   precision = of the pairs we merged, how many really are the same person   (wrong merges hurt this)
 *   recall    = of the pairs that really are the same person, how many we merged (missed duplicates hurt this)
 *
 * For a CRM, a wrong merge is worse than a missed duplicate: merging two different customers loses data
 * and can send one person's emails to someone else. So the default threshold favours precision.
 */
public final class Evaluator {

    private Evaluator() {
    }

    public record Metrics(double threshold, long truePairs, long predictedPairs, long correctPairs,
                          double precision, double recall, double f1) {
    }

    /** blocking recall = the share of real duplicate pairs that blocking let through to be compared at all */
    public record BlockingQuality(long truePairs, long truePairsCompared, double recall) {
    }

    public static void requireTruth(List<Contact> contacts) {
        for (Contact contact : contacts) {
            if (contact.trueEntityId() == null || contact.trueEntityId().isEmpty()) {
                throw new DedupeInputException("evaluate needs a true_entity_id column on every row "
                        + "(files made by the generate command have one)");
            }
        }
    }

    /** Groups the pairs that score at least the threshold, then compares those groups with the truth. */
    public static Metrics atThreshold(List<Contact> contacts, List<ScoredPair> scoredPairs, double threshold) {
        UnionFind groups = new UnionFind(contacts.size());
        for (ScoredPair pair : scoredPairs) {
            if (pair.score() >= threshold) {
                groups.union(pair.first(), pair.second());
            }
        }
        int[] predictedGroup = new int[contacts.size()];
        for (int i = 0; i < predictedGroup.length; i++) {
            predictedGroup[i] = groups.find(i);
        }
        return metrics(contacts, predictedGroup, threshold);
    }

    /**
     * Pairs are counted from group sizes instead of being listed one by one: a group of n rows contains
     * n*(n-1)/2 pairs. A pair is correct when both rows are in the same predicted group AND have the same
     * true id, so the correct pairs come from the sizes of the (predicted group, true id) combinations.
     */
    static Metrics metrics(List<Contact> contacts, int[] predictedGroup, double threshold) {
        Map<String, Long> trueSizes = new HashMap<>();
        Map<Integer, Long> predictedSizes = new HashMap<>();
        Map<String, Long> overlapSizes = new HashMap<>();
        for (int i = 0; i < contacts.size(); i++) {
            String truth = contacts.get(i).trueEntityId();
            trueSizes.merge(truth, 1L, Long::sum);
            predictedSizes.merge(predictedGroup[i], 1L, Long::sum);
            overlapSizes.merge(predictedGroup[i] + "|" + truth, 1L, Long::sum);
        }
        long truePairs = pairsIn(trueSizes.values());
        long predictedPairs = pairsIn(predictedSizes.values());
        long correctPairs = pairsIn(overlapSizes.values());

        double precision = predictedPairs == 0 ? 1.0 : (double) correctPairs / predictedPairs;
        double recall = truePairs == 0 ? 1.0 : (double) correctPairs / truePairs;
        double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);
        return new Metrics(threshold, truePairs, predictedPairs, correctPairs, precision, recall, f1);
    }

    public static BlockingQuality blockingQuality(List<Contact> contacts, long[] sortedCandidatePairs) {
        Map<String, List<Integer>> rowsByTruth = new HashMap<>();
        for (int i = 0; i < contacts.size(); i++) {
            rowsByTruth.computeIfAbsent(contacts.get(i).trueEntityId(), id -> new ArrayList<>()).add(i);
        }
        long truePairs = 0;
        long compared = 0;
        for (List<Integer> rows : rowsByTruth.values()) {
            for (int a = 0; a < rows.size(); a++) {
                for (int b = a + 1; b < rows.size(); b++) {
                    truePairs++;
                    long pair = CandidatePairs.encode(Math.min(rows.get(a), rows.get(b)), Math.max(rows.get(a), rows.get(b)));
                    if (Arrays.binarySearch(sortedCandidatePairs, pair) >= 0) {
                        compared++;
                    }
                }
            }
        }
        return new BlockingQuality(truePairs, compared, truePairs == 0 ? 1.0 : (double) compared / truePairs);
    }

    private static long pairsIn(Iterable<Long> groupSizes) {
        long pairs = 0;
        for (long size : groupSizes) {
            pairs += size * (size - 1) / 2;
        }
        return pairs;
    }
}
