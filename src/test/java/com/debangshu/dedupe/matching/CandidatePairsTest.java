package com.debangshu.dedupe.matching;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatePairsTest {

    @Test
    void duplicatesAndReversedPairsAreStoredOnce() {
        CandidatePairs pairs = new CandidatePairs();
        pairs.add(3, 1);
        pairs.add(1, 3);
        pairs.add(0, 2);
        pairs.add(1, 3);

        assertThat(pairs.toSortedUniqueArray())
                .containsExactly(CandidatePairs.encode(0, 2), CandidatePairs.encode(1, 3));
    }

    @Test
    void encodingRoundTripsLargeIndexes() {
        long pair = CandidatePairs.encode(1_999_999, 2_000_000_000);

        assertThat(CandidatePairs.first(pair)).isEqualTo(1_999_999);
        assertThat(CandidatePairs.second(pair)).isEqualTo(2_000_000_000);
    }

    @Test
    void growsPastItsStartingSize() {
        CandidatePairs pairs = new CandidatePairs();
        for (int i = 0; i < 5_000; i++) {
            pairs.add(i, i + 1);
        }

        assertThat(pairs.toSortedUniqueArray()).hasSize(5_000);
    }
}
