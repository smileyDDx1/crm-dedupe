package com.debangshu.dedupe.matching;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizerTest {

    @Test
    void textIsLowerCaseWithoutAccentsOrPunctuation() {
        assertThat(Normalizer.text("  Müller-Lüdenscheidt, Jr. ")).isEqualTo("muller ludenscheidt jr");
        assertThat(Normalizer.text("D'Souza")).isEqualTo("d souza");
        assertThat(Normalizer.text(null)).isEmpty();
        assertThat(Normalizer.text("   ")).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "'Bluepeak Software Pvt. Ltd.',      'bluepeak software'",
            "'BLUEPEAK SOFTWARE PRIVATE LIMITED', 'bluepeak software'",
            "'The Bluepeak Software Company',     'bluepeak software'",
            "'Nimbus Analytics Inc.',             'nimbus analytics'",
            "'Nimbus Analytics, LLC',             'nimbus analytics'",
            "'Limited',                           ''"
    })
    void companySuffixesAreRemoved(String raw, String expected) {
        assertThat(Normalizer.company(raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "'Rahul.Sharma@Bluepeak.com',       'rahul.sharma@bluepeak.com'",
            "'rahul.sharma+crm@bluepeak.com',   'rahul.sharma@bluepeak.com'",
            "'r.a.h.u.l.sharma@gmail.com',      'rahulsharma@gmail.com'",
            "'rahulsharma+news@GoogleMail.com', 'rahulsharma@gmail.com'",
            "'not-an-email',                    ''",
            "'@gmail.com',                      ''",
            "'rahul@',                          ''"
    })
    void emails(String raw, String expected) {
        assertThat(Normalizer.email(raw)).isEqualTo(expected);
    }

    @Test
    void dotsOnlyGetRemovedForGmail() {
        assertThat(Normalizer.email("rahul.sharma@outlook.com")).isEqualTo("rahul.sharma@outlook.com");
        assertThat(Normalizer.email(null)).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "'+91 98765 43210', '9876543210'",
            "'098765-43210',    '9876543210'",
            "'+91-9876543210',  '9876543210'",
            "'987-654-3210',    '9876543210'",
            "'12345',           ''",
            "'n/a',             ''"
    })
    void phones(String raw, String expected) {
        assertThat(Normalizer.phone(raw)).isEqualTo(expected);
    }

    @Test
    void normalizedContactPrecomputesEverything() {
        var contact = new com.debangshu.dedupe.Contact("1", "Rahul", "Sharma", "Rahul.Sharma+x@Gmail.com",
                "+91 98765 43210", "Bluepeak Pvt Ltd", " Pune ", null, null);

        NormalizedContact normalized = NormalizedContact.of(contact);

        assertThat(normalized.fullName()).isEqualTo("rahul sharma");
        assertThat(normalized.reversedName()).isEqualTo("sharma rahul");
        assertThat(normalized.email()).isEqualTo("rahulsharma@gmail.com");
        assertThat(normalized.emailUser()).isEqualTo("rahulsharma");
        assertThat(normalized.phone()).isEqualTo("9876543210");
        assertThat(normalized.company()).isEqualTo("bluepeak");
        assertThat(normalized.city()).isEqualTo("pune");
    }
}
