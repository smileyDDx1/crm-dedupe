package com.debangshu.dedupe.matching;

import com.debangshu.dedupe.Contact;
import org.junit.jupiter.api.Test;

import static com.debangshu.dedupe.TestContacts.contact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PairScorerTest {

    private final PairScorer scorer = new PairScorer();

    private double score(Contact a, Contact b) {
        return scorer.score(NormalizedContact.of(a), NormalizedContact.of(b));
    }

    @Test
    void sameEmailWithATypoInTheNameIsAMatch() {
        Contact a = contact("1", "Rahul", "Sharma", "rahul.sharma@bluepeak.com", "", "Bluepeak Software Pvt Ltd");
        Contact b = contact("2", "Rahul", "Shrama", "RAHUL.SHARMA@BLUEPEAK.COM", "", "Bluepeak Software Private Limited");

        assertThat(score(a, b)).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void samePhoneInAnotherFormatIsAMatchEvenWithADifferentEmail() {
        Contact work = contact("1", "Priya", "Patel", "priya.patel@nimbus.in", "+91 98765 43210", "Nimbus Analytics");
        Contact personal = contact("2", "Priya", "Patel", "priyapatel@gmail.com", "09876543210", "Nimbus Analytics Inc");

        assertThat(score(work, personal)).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void sameNameButEverythingElseDifferentIsNotAMatch() {
        // two different people called Rahul Sharma
        Contact a = contact("1", "Rahul", "Sharma", "rahul.sharma@bluepeak.com", "9876543210", "Bluepeak Software");
        Contact b = contact("2", "Rahul", "Sharma", "rahul.sharma@orbitretail.in", "9123456780", "Orbit Retail");

        assertThat(score(a, b)).isLessThan(0.70);
    }

    @Test
    void nameAndCompanyWithoutEmailOrPhoneGoesToReview() {
        Contact a = contact("1", "Rahul", "Sharma", "", "", "Bluepeak Software Pvt Ltd");
        Contact b = contact("2", "Rahul", "Sharma", "rahul@bluepeak.com", "9876543210", "BLUEPEAK SOFTWARE LIMITED");

        // name 0.60 + company 0.15 + city 0.05: in the "maybe" band, not merged
        assertThat(score(a, b)).isCloseTo(0.80, within(1e-9));
    }

    @Test
    void sharedInboxDoesNotMergeDifferentNames() {
        Contact a = contact("1", "Rahul", "Sharma", "info@bluepeak.com", "", "Bluepeak Software");
        Contact b = contact("2", "Meera", "Iyer", "info@bluepeak.com", "", "Bluepeak Software");

        assertThat(score(a, b)).isLessThan(0.70);
    }

    @Test
    void emptyFieldsCountNeitherForNorAgainst() {
        Contact a = new Contact("1", "Rahul", "Sharma", "", "", "", "", null, null);
        Contact b = new Contact("2", "Rahul", "Sharma", "rahul@bluepeak.com", "9876543210", "Bluepeak", "Pune", null, null);

        assertThat(score(a, b)).isCloseTo(PairScorer.NAME_POINTS, within(1e-9));
    }

    @Test
    void swappedFirstAndLastNameStillCountsAsTheSameName() {
        Contact a = contact("1", "Rahul", "Sharma", "rahul@bluepeak.com", "", "");
        Contact b = contact("2", "Sharma", "Rahul", "rahul@bluepeak.com", "", "");

        assertThat(score(a, b)).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void scoreStaysBetweenZeroAndOne() {
        Contact a = contact("1", "Rahul", "Sharma", "rahul@bluepeak.com", "9876543210", "Bluepeak");
        Contact sameEverything = contact("2", "Rahul", "Sharma", "rahul@bluepeak.com", "9876543210", "Bluepeak");
        Contact differentEverything = new Contact("3", "Zoya", "Khan", "zoya@orbit.in", "9000000001", "Orbit Retail", "Pune", null, null);

        assertThat(score(a, sameEverything)).isEqualTo(1.0);
        assertThat(score(a, differentEverything)).isBetween(0.0, 0.5);
    }
}
