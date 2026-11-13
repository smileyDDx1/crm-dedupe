package com.debangshu.dedupe.matching;

import com.debangshu.dedupe.Contact;

/**
 * A contact after normalization. Built once per row, then reused for every comparison
 * (a row can be compared hundreds of times, normalizing it every time would be wasted work).
 */
public record NormalizedContact(Contact source, String firstName, String lastName, String fullName,
                                String reversedName, String email, String emailUser, String phone,
                                String company, String city) {

    public static NormalizedContact of(Contact contact) {
        String first = Normalizer.name(contact.firstName());
        String last = Normalizer.name(contact.lastName());
        String email = Normalizer.email(contact.email());
        String emailUser = email.isEmpty() ? "" : email.substring(0, email.indexOf('@'));
        return new NormalizedContact(contact, first, last, (first + " " + last).trim(), (last + " " + first).trim(),
                email, emailUser, Normalizer.phone(contact.phone()), Normalizer.company(contact.company()),
                Normalizer.text(contact.city()));
    }
}
