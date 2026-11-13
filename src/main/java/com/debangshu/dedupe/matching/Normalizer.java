package com.debangshu.dedupe.matching;

import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Puts values into one standard form before comparing them, so that differences that don't matter
 * ("ACME Pvt. Ltd." vs "Acme Private Limited", "+91 98765-43210" vs "09876543210") disappear.
 * Most duplicates in a CRM are not typos, they are the same value written differently.
 */
public final class Normalizer {

    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_LETTER_OR_DIGIT = Pattern.compile("[^a-z0-9]+");
    private static final Pattern NOT_DIGIT = Pattern.compile("[^0-9]");

    /** Words that say what kind of company it is, not which company. */
    private static final Set<String> COMPANY_SUFFIXES = Set.of(
            "the", "inc", "incorporated", "llc", "llp", "ltd", "limited", "pvt", "private", "corp",
            "corporation", "co", "company", "gmbh", "plc", "pte", "pty");

    private Normalizer() {
    }

    /** lower case, accents removed ("Müller" -> "muller"), punctuation turned into single spaces */
    public static String text(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        // NFKD splits "ü" into "u" + a separate accent mark, then the marks are removed
        String withoutAccents = ACCENTS.matcher(java.text.Normalizer.normalize(value, Form.NFKD)).replaceAll("");
        return NOT_LETTER_OR_DIGIT.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    public static String name(String value) {
        return text(value);
    }

    /** "The Acme Software Pvt. Ltd." -> "acme software" */
    public static String company(String value) {
        return Arrays.stream(text(value).split(" "))
                .filter(word -> !word.isEmpty() && !COMPANY_SUFFIXES.contains(word))
                .collect(Collectors.joining(" "));
    }

    /**
     * Lower case, "+tag" removed ("rahul+crm@x.com" -> "rahul@x.com"). Gmail also ignores dots in the
     * user name, so "r.a.h.u.l@gmail.com" and "rahul@gmail.com" are the same inbox.
     * Returns "" for something that isn't an email address.
     */
    public static String email(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String email = value.trim().toLowerCase(Locale.ROOT);
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "";
        }
        String user = email.substring(0, at);
        String domain = email.substring(at + 1);
        int plus = user.indexOf('+');
        if (plus >= 0) {
            user = user.substring(0, plus);
        }
        if (domain.equals("googlemail.com")) {
            domain = "gmail.com";
        }
        if (domain.equals("gmail.com")) {
            user = user.replace(".", "");
        }
        return user.isEmpty() ? "" : user + "@" + domain;
    }

    /**
     * Digits only, and only the last 10, so "+91 98765 43210", "098765-43210" and "9876543210" match.
     * (10 digits is a mobile number in India; for other countries this would need a real phone library.)
     */
    public static String phone(String value) {
        if (value == null) {
            return "";
        }
        String digits = NOT_DIGIT.matcher(value).replaceAll("");
        if (digits.length() < 7) {
            return ""; // too short to be a phone number
        }
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }
}
