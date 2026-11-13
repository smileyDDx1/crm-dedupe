package com.debangshu.dedupe.pipeline;

import com.debangshu.dedupe.Contact;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Builds the one merged row ("golden record") for a group of duplicates. The rules (called
 * "survivorship rules" in data quality tools):
 *
 * 1. Rank the rows: most recently updated first, then the most complete, then the lowest id.
 * 2. The top row gives the id that survives.
 * 3. Every field takes the first non-empty value in that ranking. So an old row can still fill in a
 *    phone number that the newest row is missing.
 */
public final class GoldenRecordBuilder {

    static final Comparator<Contact> BEST_FIRST = Comparator
            .comparing(Contact::updatedAt, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
            .thenComparing(Contact::filledFields, Comparator.reverseOrder())
            .thenComparing(Contact::id);

    private GoldenRecordBuilder() {
    }

    public static Contact build(List<Contact> members) {
        List<Contact> ranked = members.stream().sorted(BEST_FIRST).toList();
        Contact best = ranked.getFirst();
        LocalDate newest = ranked.stream().map(Contact::updatedAt).filter(Objects::nonNull).findFirst().orElse(null);
        return new Contact(best.id(),
                firstFilled(ranked, Contact::firstName),
                firstFilled(ranked, Contact::lastName),
                firstFilled(ranked, Contact::email),
                firstFilled(ranked, Contact::phone),
                firstFilled(ranked, Contact::company),
                firstFilled(ranked, Contact::city),
                newest,
                best.trueEntityId());
    }

    private static String firstFilled(List<Contact> ranked, Function<Contact, String> field) {
        for (Contact contact : ranked) {
            String value = field.apply(contact);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }
}
