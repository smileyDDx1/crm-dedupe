package com.debangshu.dedupe;

import java.time.LocalDate;

/** Short ways to build contacts in tests. */
public final class TestContacts {

    private TestContacts() {
    }

    /** A contact in Bengaluru with no update date and no truth id. */
    public static Contact contact(String id, String first, String last, String email, String phone, String company) {
        return new Contact(id, first, last, email, phone, company, "Bengaluru", null, null);
    }

    public static Contact contact(String id, String first, String last, String email, String phone, String company,
                                  String city, LocalDate updatedAt, String trueEntityId) {
        return new Contact(id, first, last, email, phone, company, city, updatedAt, trueEntityId);
    }
}
