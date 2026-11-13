package com.debangshu.dedupe.matching;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SoundexTest {

    // standard examples for American Soundex
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "Robert,   R163",
            "Rupert,   R163",
            "Rubin,    R150",
            "Ashcraft, A261", // H between two letters with the same code doesn't separate them
            "Ashcroft, A261",
            "Tymczak,  T522",
            "Pfister,  P236", // P and F have the same code, so the F isn't coded again
            "Honeyman, H555",
            "Sharma,   S650",
            "Sarma,    S650"
    })
    void standardCodes(String name, String expected) {
        assertThat(Soundex.encode(name)).isEqualTo(expected);
    }

    @Test
    void ignoresCaseAndNonLetters() {
        assertThat(Soundex.encode("d'souza")).isEqualTo(Soundex.encode("DSOUZA"));
        assertThat(Soundex.encode("Lee")).isEqualTo("L000");
    }

    @Test
    void emptyInputGivesAnEmptyCode() {
        assertThat(Soundex.encode("")).isEmpty();
        assertThat(Soundex.encode("123")).isEmpty();
    }
}
