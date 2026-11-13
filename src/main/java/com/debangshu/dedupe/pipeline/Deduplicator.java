package com.debangshu.dedupe.pipeline;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.matching.Blocker;
import com.debangshu.dedupe.matching.CandidatePairs;
import com.debangshu.dedupe.matching.NormalizedContact;
import com.debangshu.dedupe.matching.PairScorer;
import com.debangshu.dedupe.matching.UnionFind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * The whole pipeline:
 *
 *   normalize -> block (find candidate pairs) -> score each pair -> group matches (union-find) -> golden records
 *
 * Scoring is the expensive step and every pair is independent of the others, so it runs as a parallel
 * stream (one chunk of pairs per CPU core). Nothing is shared between the scoring threads except the
 * read-only list of normalized rows, so no locks are needed.
 */
public final class Deduplicator {

    private final MatchSettings settings;
    private final Blocker blocker;
    private final PairScorer scorer = new PairScorer();

    public Deduplicator(MatchSettings settings) {
        this.settings = settings;
        this.blocker = new Blocker(settings.maxBlockSize());
    }

    public DedupeResult run(List<Contact> contacts) {
        int rows = contacts.size();
        long allPairs = (long) rows * (rows - 1) / 2;

        long start = System.nanoTime();
        List<NormalizedContact> normalized = contacts.parallelStream().map(NormalizedContact::of).toList();
        long normalizedAt = System.nanoTime();

        long[] candidates = null;
        int blocks = 0;
        int skippedBlocks = 0;
        if (!settings.exhaustive()) {
            Blocker.Result blocking = blocker.candidatePairs(normalized);
            candidates = blocking.pairs();
            blocks = blocking.blocks();
            skippedBlocks = blocking.skippedBlocks();
        }
        long blockedAt = System.nanoTime();

        List<ScoredPair> scored = candidates == null ? scoreAllPairs(normalized) : scoreCandidates(normalized, candidates);
        long scoredAt = System.nanoTime();

        UnionFind groups = new UnionFind(rows);
        int matchPairs = 0;
        for (ScoredPair pair : scored) {
            if (pair.score() >= settings.matchThreshold()) {
                groups.union(pair.first(), pair.second());
                matchPairs++;
            }
        }
        // only "maybe" pairs that didn't end up in the same group anyway (through other matches) need a human
        List<ScoredPair> review = scored.stream()
                .filter(pair -> pair.score() < settings.matchThreshold())
                .filter(pair -> !groups.connected(pair.first(), pair.second()))
                .sorted(Comparator.comparingDouble(ScoredPair::score).reversed())
                .toList();
        List<Cluster> clusters = buildClusters(contacts, groups);
        long clusteredAt = System.nanoTime();

        int duplicateRows = clusters.stream().mapToInt(cluster -> cluster.members().size() - 1).sum();
        DedupeStats stats = new DedupeStats(rows, allPairs, candidates == null ? allPairs : candidates.length,
                blocks, skippedBlocks, matchPairs, review.size(), clusters.size(), duplicateRows,
                millis(start, normalizedAt), millis(normalizedAt, blockedAt), millis(blockedAt, scoredAt),
                millis(scoredAt, clusteredAt));
        return new DedupeResult(contacts, clusters, review, scored, candidates, stats);
    }

    private List<ScoredPair> scoreCandidates(List<NormalizedContact> rows, long[] candidates) {
        LongStream pairs = Arrays.stream(candidates);
        if (settings.parallel()) {
            pairs = pairs.parallel();
        }
        return pairs
                .mapToObj(pair -> scoreOrNull(rows, CandidatePairs.first(pair), CandidatePairs.second(pair)))
                .filter(Objects::nonNull)
                .toList();
    }

    /** Every row against every other row. O(n^2): only for measuring what blocking misses on small files. */
    private List<ScoredPair> scoreAllPairs(List<NormalizedContact> rows) {
        int size = rows.size();
        Stream<Integer> firstRows = IntStream.range(0, size).boxed();
        if (settings.parallel()) {
            firstRows = firstRows.parallel();
        }
        return firstRows
                .flatMap(a -> IntStream.range(a + 1, size).mapToObj(b -> scoreOrNull(rows, a, b)).filter(Objects::nonNull))
                .toList();
    }

    private ScoredPair scoreOrNull(List<NormalizedContact> rows, int a, int b) {
        double score = scorer.score(rows.get(a), rows.get(b));
        return score >= settings.reviewThreshold() ? new ScoredPair(a, b, score) : null;
    }

    private static List<Cluster> buildClusters(List<Contact> contacts, UnionFind groups) {
        List<Cluster> clusters = new ArrayList<>();
        int id = 1;
        for (List<Integer> group : groups.groups()) {
            List<Contact> members = group.stream().map(contacts::get).toList();
            clusters.add(new Cluster(id++, members, GoldenRecordBuilder.build(members)));
        }
        return clusters;
    }

    private static long millis(long fromNanos, long toNanos) {
        return (toNanos - fromNanos) / 1_000_000;
    }
}
