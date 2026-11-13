package com.debangshu.dedupe.pipeline;

import com.debangshu.dedupe.Contact;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.debangshu.dedupe.TestContacts.contact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeduplicatorTest {

    /** Rahul is in the CRM three times, written three different ways; Priya and the other Rahul once each. */
    private static final List<Contact> CRM = List.of(
            contact("C1", "Rahul", "Sharma", "rahul.sharma@bluepeak.com", "+91 98765 43210", "Bluepeak Software Pvt Ltd",
                    "Pune", LocalDate.of(2025, 2, 1), null),
            contact("C2", "Priya", "Patel", "priya.patel@nimbus.in", "9123456780", "Nimbus Analytics",
                    "Mumbai", LocalDate.of(2026, 1, 1), null),
            contact("C3", "RAHUL", "SHRAMA", "Rahul.Sharma@Bluepeak.com", "", "BLUEPEAK SOFTWARE PRIVATE LIMITED",
                    "Pune", LocalDate.of(2026, 5, 1), null),
            contact("C4", "Rahul", "Sharma", "rahulsharma@gmail.com", "098765-43210", "",
                    "", LocalDate.of(2024, 7, 1), null),
            // a different Rahul Sharma: same name, nothing else in common
            contact("C5", "Rahul", "Sharma", "rahul.sharma@orbitretail.in", "9000000001", "Orbit Retail",
                    "Delhi", LocalDate.of(2026, 2, 2), null));

    @Test
    void findsTheThreeRowsOfTheSamePersonAndKeepsTheOthersApart() {
        DedupeResult result = new Deduplicator(MatchSettings.defaults()).run(CRM);

        assertThat(result.clusters()).hasSize(1);
        Cluster rahul = result.clusters().getFirst();
        assertThat(rahul.members()).extracting(Contact::id).containsExactly("C1", "C3", "C4");
        assertThat(rahul.golden().id()).isEqualTo("C3"); // most recently updated
        assertThat(rahul.golden().phone()).isEqualTo("+91 98765 43210"); // filled in from C1

        DedupeStats stats = result.stats();
        assertThat(stats.rows()).isEqualTo(5);
        assertThat(stats.duplicateRows()).isEqualTo(2);
        assertThat(stats.comparedPairs()).isLessThan(stats.allPairs());
    }

    @Test
    void pairsInTheMaybeBandGoToReview() {
        List<Contact> rows = List.of(
                contact("C1", "Meera", "Iyer", "", "", "Kaveri Textiles Ltd"),
                contact("C2", "Meera", "Iyer", "meera@kaveri.com", "9876501234", "KAVERI TEXTILES"));

        DedupeResult result = new Deduplicator(MatchSettings.defaults()).run(rows);

        assertThat(result.clusters()).isEmpty();
        assertThat(result.reviewPairs()).hasSize(1);
        assertThat(result.reviewPairs().getFirst().score()).isBetween(0.70, 0.85);
    }

    @Test
    void exhaustiveModeComparesEveryPairAndFindsTheSameGroups() {
        MatchSettings exhaustive = new MatchSettings(0.85, 0.70, 1000, true, true);

        DedupeResult result = new Deduplicator(exhaustive).run(CRM);

        assertThat(result.stats().comparedPairs()).isEqualTo(10); // 5 rows -> 10 pairs
        assertThat(result.candidatePairs()).isNull();
        assertThat(result.clusters().getFirst().members()).extracting(Contact::id).containsExactly("C1", "C3", "C4");
    }

    @Test
    void sequentialScoringGivesTheSameResult() {
        MatchSettings sequential = new MatchSettings(0.85, 0.70, 1000, false, false);

        DedupeResult parallel = new Deduplicator(MatchSettings.defaults()).run(CRM);
        DedupeResult oneThread = new Deduplicator(sequential).run(CRM);

        assertThat(oneThread.clusters()).isEqualTo(parallel.clusters());
    }

    @Test
    void emptyInputIsFine() {
        DedupeResult result = new Deduplicator(MatchSettings.defaults()).run(List.of());

        assertThat(result.clusters()).isEmpty();
        assertThat(result.stats().allPairs()).isZero();
    }

    @Test
    void settingsAreValidated() {
        assertThatThrownBy(() -> new MatchSettings(1.5, 0.7, 1000, false, true)).hasMessageContaining("between 0 and 1");
        assertThatThrownBy(() -> new MatchSettings(0.7, 0.9, 1000, false, true)).hasMessageContaining("review threshold");
        assertThatThrownBy(() -> new MatchSettings(0.85, 0.7, 1, false, true)).hasMessageContaining("block size");
    }
}
