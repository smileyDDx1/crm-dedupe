package com.debangshu.dedupe.matching;

import com.debangshu.dedupe.Contact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.debangshu.dedupe.TestContacts.contact;
import static org.assertj.core.api.Assertions.assertThat;

class BlockerTest {

    private static List<NormalizedContact> normalized(Contact... contacts) {
        return java.util.Arrays.stream(contacts).map(NormalizedContact::of).toList();
    }

    @Test
    void aFullContactGetsFourKeys() {
        NormalizedContact rahul = NormalizedContact.of(contact("1", "Rahul", "Sharma", "rahul@x.com", "9876543210", "X"));

        assertThat(Blocker.keysFor(rahul)).containsExactly("e:rahul@x.com", "p:9876543210", "n1:S650r", "n2:R400s");
    }

    @Test
    void missingFieldsGiveFewerKeys() {
        NormalizedContact noName = NormalizedContact.of(contact("1", "", "", "rahul@x.com", "", "X"));

        assertThat(Blocker.keysFor(noName)).containsExactly("e:rahul@x.com");
    }

    @Test
    void rowsThatShareAKeyBecomeOneCandidatePair() {
        List<NormalizedContact> rows = normalized(
                contact("1", "Rahul", "Sharma", "rahul@x.com", "", "X"),
                contact("2", "Rahul", "Sarma", "rahul@x.com", "", "X"), // shares the email AND the soundex key
                contact("3", "Priya", "Patel", "priya@y.com", "", "Y"));

        Blocker.Result result = new Blocker(100).candidatePairs(rows);

        assertThat(result.pairs()).containsExactly(CandidatePairs.encode(0, 1)); // once, not twice
        assertThat(result.skippedBlocks()).isZero();
    }

    @Test
    void oversizedBlocksAreSkipped() {
        // five different people with the same office phone number
        List<NormalizedContact> rows = normalized(
                contact("1", "Aarav", "Shah", "", "9876543210", ""),
                contact("2", "Ritu", "Iyer", "", "9876543210", ""),
                contact("3", "Yash", "Rao", "", "9876543210", ""),
                contact("4", "Meera", "Nair", "", "9876543210", ""),
                contact("5", "Imran", "Khan", "", "9876543210", ""));

        Blocker.Result result = new Blocker(3).candidatePairs(rows);

        assertThat(result.pairs()).isEmpty();
        assertThat(result.skippedBlocks()).isEqualTo(1);
    }
}
