package com.debangshu.dedupe.evaluation;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.DedupeInputException;
import com.debangshu.dedupe.evaluation.Evaluator.BlockingQuality;
import com.debangshu.dedupe.evaluation.Evaluator.Metrics;
import com.debangshu.dedupe.matching.CandidatePairs;
import com.debangshu.dedupe.pipeline.ScoredPair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.debangshu.dedupe.TestContacts.contact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class EvaluatorTest {

    /** Five rows: A appears three times, B and C once. */
    private static List<Contact> rows(String... truth) {
        return java.util.stream.IntStream.range(0, truth.length)
                .mapToObj(i -> contact("C" + i, "x", "y", "", "", "", "", null, truth[i]))
                .toList();
    }

    @Test
    void precisionAndRecallAreCountedInPairs() {
        // truth:     A A A B C   -> 3 real pairs (all inside A)
        // predicted: 0 0 1 1 2   -> 2 merged pairs: (row0,row1) correct, (row2,row3) wrong
        Metrics m = Evaluator.metrics(rows("A", "A", "A", "B", "C"), new int[]{0, 0, 1, 1, 2}, 0.85);

        assertThat(m.truePairs()).isEqualTo(3);
        assertThat(m.predictedPairs()).isEqualTo(2);
        assertThat(m.correctPairs()).isEqualTo(1);
        assertThat(m.precision()).isEqualTo(0.5);
        assertThat(m.recall()).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(m.f1()).isCloseTo(0.4, within(1e-9));
    }

    @Test
    void thresholdDecidesWhichPairsAreGrouped() {
        List<Contact> contacts = rows("A", "A", "A", "B", "B");
        List<ScoredPair> scored = List.of(new ScoredPair(0, 1, 0.90), new ScoredPair(1, 2, 0.80), new ScoredPair(3, 4, 0.95));

        Metrics strict = Evaluator.atThreshold(contacts, scored, 0.85);
        Metrics loose = Evaluator.atThreshold(contacts, scored, 0.75);

        assertThat(strict.precision()).isEqualTo(1.0);
        assertThat(strict.recall()).isEqualTo(0.5);  // 2 of the 4 real pairs
        assertThat(loose.recall()).isEqualTo(1.0);   // 0-1-2 joined through row 1
    }

    @Test
    void nothingMergedMeansPerfectPrecisionAndZeroRecall() {
        Metrics m = Evaluator.metrics(rows("A", "A"), new int[]{0, 1}, 0.9);

        assertThat(m.precision()).isEqualTo(1.0);
        assertThat(m.recall()).isZero();
        assertThat(m.f1()).isZero();
    }

    @Test
    void blockingQualityIsTheShareOfRealPairsThatWereCompared() {
        List<Contact> contacts = rows("A", "A", "B", "B");
        long[] candidates = {CandidatePairs.encode(0, 1), CandidatePairs.encode(1, 2)};

        BlockingQuality quality = Evaluator.blockingQuality(contacts, candidates);

        assertThat(quality.truePairs()).isEqualTo(2);
        assertThat(quality.truePairsCompared()).isEqualTo(1);
        assertThat(quality.recall()).isEqualTo(0.5);
    }

    @Test
    void evaluationNeedsTheTruthColumn() {
        List<Contact> noTruth = List.of(contact("C1", "Rahul", "Sharma", "", "", ""));

        assertThatThrownBy(() -> Evaluator.requireTruth(noTruth))
                .isInstanceOf(DedupeInputException.class)
                .hasMessageContaining("true_entity_id");
    }
}
