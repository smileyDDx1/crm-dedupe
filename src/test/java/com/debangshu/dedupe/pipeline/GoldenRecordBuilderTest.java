package com.debangshu.dedupe.pipeline;

import com.debangshu.dedupe.Contact;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.debangshu.dedupe.TestContacts.contact;
import static org.assertj.core.api.Assertions.assertThat;

class GoldenRecordBuilderTest {

    @Test
    void newestRowWinsAndOlderRowsFillTheGaps() {
        Contact old = contact("C1", "Rahul", "Sharma", "rahul@bluepeak.com", "+91 98765 43210", "Bluepeak Pvt Ltd",
                "Pune", LocalDate.of(2025, 1, 10), null);
        Contact newest = contact("C2", "Rahul", "Sharma", "", "", "Bluepeak Software Pvt Ltd",
                "Bengaluru", LocalDate.of(2026, 6, 1), null);

        Contact golden = GoldenRecordBuilder.build(List.of(old, newest));

        assertThat(golden.id()).isEqualTo("C2");
        assertThat(golden.city()).isEqualTo("Bengaluru");           // from the newest row
        assertThat(golden.company()).isEqualTo("Bluepeak Software Pvt Ltd");
        assertThat(golden.email()).isEqualTo("rahul@bluepeak.com"); // newest had none, so the older one fills it
        assertThat(golden.phone()).isEqualTo("+91 98765 43210");
        assertThat(golden.updatedAt()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void onTheSameDateTheMoreCompleteRowWins() {
        LocalDate day = LocalDate.of(2026, 3, 3);
        Contact sparse = contact("C1", "Priya", "Patel", "", "", "", "", day, null);
        Contact complete = contact("C2", "Priya", "Patel", "priya@nimbus.in", "9876543210", "Nimbus", "Pune", day, null);

        assertThat(GoldenRecordBuilder.build(List.of(sparse, complete)).id()).isEqualTo("C2");
    }

    @Test
    void rowsWithoutADateComeLast() {
        Contact undated = contact("C1", "Priya", "Patel", "priya@nimbus.in", "9876543210", "Nimbus", "Pune", null, null);
        Contact dated = contact("C2", "Priya", "Patel", "", "", "", "", LocalDate.of(2020, 1, 1), null);

        Contact golden = GoldenRecordBuilder.build(List.of(undated, dated));

        assertThat(golden.id()).isEqualTo("C2");
        assertThat(golden.email()).isEqualTo("priya@nimbus.in");
    }
}
