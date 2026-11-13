package com.debangshu.dedupe.generator;

import com.debangshu.dedupe.Contact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ContactGeneratorTest {

    @Test
    void sameSeedGivesTheSameFile() {
        assertThat(new ContactGenerator(7).generate(500, 0.3)).isEqualTo(new ContactGenerator(7).generate(500, 0.3));
        assertThat(new ContactGenerator(7).generate(500, 0.3)).isNotEqualTo(new ContactGenerator(8).generate(500, 0.3));
    }

    @Test
    void everyRowHasAUniqueIdAndKnowsItsPerson() {
        List<Contact> rows = new ContactGenerator(1).generate(2_000, 0.25);

        Set<String> ids = rows.stream().map(Contact::id).collect(Collectors.toSet());
        Set<String> people = rows.stream().map(Contact::trueEntityId).collect(Collectors.toSet());
        assertThat(ids).hasSize(rows.size());
        assertThat(people).hasSize(2_000);
    }

    @Test
    void duplicateRateControlsHowManyExtraRowsThereAre() {
        assertThat(new ContactGenerator(1).generate(1_000, 0.0)).hasSize(1_000);

        // 25% of 2,000 people x 1.4 extra rows on average = about 700
        int extraRows = new ContactGenerator(1).generate(2_000, 0.25).size() - 2_000;
        assertThat(extraRows).isBetween(550, 850);
    }

    @Test
    void differentPeopleNeverShareAnEmailOrPhone() {
        List<Contact> rows = new ContactGenerator(3).generate(3_000, 0.0); // one row per person

        long distinctEmails = rows.stream().map(c -> c.email().toLowerCase()).distinct().count();
        long distinctPhones = rows.stream().map(c -> c.phone().replaceAll("\\D", "")).map(p -> p.substring(p.length() - 10)).distinct().count();
        assertThat(distinctEmails).isEqualTo(3_000);
        assertThat(distinctPhones).isEqualTo(3_000);
    }

    @Test
    void typosChangeTheWordButKeepItRecognisable() {
        Random random = new Random(5);
        for (int i = 0; i < 200; i++) {
            String typo = Typos.typo("Sharma", random);
            assertThat(typo.length()).isBetween(5, 7);
        }
        assertThat(Typos.typo("Li", random)).isEqualTo("Li"); // too short to change
    }
}
