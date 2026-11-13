package com.debangshu.dedupe.matching;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JaroWinklerTest {

    // the classic examples from Winkler's papers (also on Wikipedia)
    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource({
            "MARTHA, MARHTA,   0.961",
            "DWAYNE, DUANE,    0.840",
            "DIXON,  DICKSONX, 0.813"
    })
    void knownValues(String a, String b, double expected) {
        assertThat(JaroWinkler.similarity(a, b)).isCloseTo(expected, within(0.001));
    }

    @Test
    void jaroWithoutThePrefixBonus() {
        assertThat(JaroWinkler.jaro("MARTHA", "MARHTA")).isCloseTo(0.944, within(0.001));
    }

    @Test
    void identicalAndEmptyStrings() {
        assertThat(JaroWinkler.similarity("sharma", "sharma")).isEqualTo(1.0);
        assertThat(JaroWinkler.similarity("", "")).isEqualTo(1.0);
        assertThat(JaroWinkler.similarity("sharma", "")).isZero();
        assertThat(JaroWinkler.similarity("abc", "xyz")).isZero();
    }

    @Test
    void isSymmetric() {
        assertThat(JaroWinkler.similarity("rahul sharma", "rahul shrama"))
                .isEqualTo(JaroWinkler.similarity("rahul shrama", "rahul sharma"));
    }

    @Test
    void typoScoresHigherThanADifferentName() {
        double typo = JaroWinkler.similarity("priya patel", "priya patle");
        double differentPerson = JaroWinkler.similarity("priya patel", "rohan menon");

        assertThat(typo).isGreaterThan(0.95);
        assertThat(differentPerson).isLessThan(0.7);
    }
}
