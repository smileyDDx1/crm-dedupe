package com.debangshu.dedupe;

import java.time.LocalDate;

/**
 * One row of the CRM export. Missing values are empty strings (never null), so the rest of the code
 * doesn't need null checks everywhere. updatedAt and trueEntityId can be null.
 *
 * trueEntityId only exists in generated test data: it says which real person a row belongs to,
 * so the evaluate command can measure how many duplicates were found correctly.
 */
public record Contact(String id, String firstName, String lastName, String email, String phone,
                      String company, String city, LocalDate updatedAt, String trueEntityId) {

    public Contact {
        id = clean(id);
        firstName = clean(firstName);
        lastName = clean(lastName);
        email = clean(email);
        phone = clean(phone);
        company = clean(company);
        city = clean(city);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public String fullName() {
        return (firstName + " " + lastName).trim();
    }

    /** How many of the contact fields are filled in. Used to pick the best record of a duplicate group. */
    public int filledFields() {
        int count = 0;
        for (String value : new String[]{firstName, lastName, email, phone, company, city}) {
            if (!value.isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
