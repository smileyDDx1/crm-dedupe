package com.debangshu.dedupe.generator;

import com.debangshu.dedupe.Contact;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Generates a messy CRM export where the right answer is known: every row has a true_entity_id saying
 * which real person it belongs to. That's what makes it possible to MEASURE precision and recall,
 * which you can't do on a real CRM without labelling thousands of rows by hand.
 *
 * Duplicates get the kinds of differences seen in real CRMs: typos, upper case, a personal email instead
 * of the work email, Gmail dots and +tags, phone formats, company suffixes, missing fields.
 * The same seed always gives the same file.
 */
public final class ContactGenerator {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    private final Random random;
    private final Set<String> usedEmails = new HashSet<>();
    private final Set<String> usedPhones = new HashSet<>();

    public ContactGenerator(long seed) {
        this.random = new Random(seed);
    }

    /** One real person; the rows are different ways this person was entered into the CRM. */
    private record Person(String entityId, String firstName, String lastName, String company, String city,
                          String phone, String workEmail, String personalEmail) {
    }

    /**
     * @param people        how many different real people
     * @param duplicateRate the share of people who appear more than once (each gets 1 to 3 extra rows)
     */
    public List<Contact> generate(int people, double duplicateRate) {
        List<Contact> rows = new ArrayList<>();
        for (int i = 1; i <= people; i++) {
            Person person = newPerson(i);
            rows.add(firstRow(person));
            if (random.nextDouble() < duplicateRate) {
                int copies = 1 + (random.nextDouble() < 0.3 ? 1 : 0) + (random.nextDouble() < 0.1 ? 1 : 0);
                for (int copy = 0; copy < copies; copy++) {
                    rows.add(messyCopy(person));
                }
            }
        }
        // shuffle, then number the rows, so duplicates aren't next to each other in the file or by id
        Collections.shuffle(rows, random);
        List<Contact> numbered = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Contact row = rows.get(i);
            numbered.add(new Contact("C%06d".formatted(i + 1), row.firstName(), row.lastName(), row.email(),
                    row.phone(), row.company(), row.city(), row.updatedAt(), row.trueEntityId()));
        }
        return numbered;
    }

    private Person newPerson(int number) {
        String first = pick(SampleData.FIRST_NAMES);
        String last = pick(SampleData.LAST_NAMES);
        String company = pick(SampleData.COMPANIES);
        String domain = company.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "") + (random.nextBoolean() ? ".com" : ".in");
        String user = emailUser(first) + "." + emailUser(last);
        String personalUser = switch (random.nextInt(4)) {
            case 0 -> emailUser(first) + emailUser(last);
            case 1 -> emailUser(first) + "." + emailUser(last) + (10 + random.nextInt(90));
            case 2 -> emailUser(first) + "_" + emailUser(last);
            default -> emailUser(first).charAt(0) + emailUser(last);
        };
        return new Person("P%06d".formatted(number), first, last, company, pick(SampleData.CITIES), uniquePhone(),
                uniqueEmail(user, domain), uniqueEmail(personalUser, pick(SampleData.PERSONAL_DOMAINS)));
    }

    private Contact firstRow(Person person) {
        String email = random.nextDouble() < 0.7 ? person.workEmail() : person.personalEmail();
        return new Contact(null, person.firstName(), person.lastName(), email, formatPhone(person.phone()),
                withSuffix(person.company()), person.city(), randomDate(), person.entityId());
    }

    private Contact messyCopy(Person person) {
        String first = person.firstName();
        String last = person.lastName();
        if (random.nextDouble() < 0.25) {
            first = Typos.typo(first, random);
        }
        if (random.nextDouble() < 0.25) {
            last = Typos.typo(last, random);
        }
        if (random.nextDouble() < 0.1) {
            first = first.toUpperCase(Locale.ROOT);
            last = last.toUpperCase(Locale.ROOT);
        }

        String email = "";
        double emailRoll = random.nextDouble();
        if (emailRoll >= 0.15) { // 15% of copies have no email
            email = emailRoll < 0.45 ? person.personalEmail() : person.workEmail();
            if (random.nextDouble() < 0.2) {
                email = email.toUpperCase(Locale.ROOT);
            }
            if (email.toLowerCase(Locale.ROOT).endsWith("@gmail.com") && random.nextDouble() < 0.4) {
                email = gmailVariant(email);
            }
        }

        String phone = random.nextDouble() < 0.2 ? "" : formatPhone(person.phone());

        String company = "";
        if (random.nextDouble() >= 0.1) {
            company = withSuffix(person.company());
            if (random.nextDouble() < 0.2) {
                company = random.nextBoolean() ? company.toUpperCase(Locale.ROOT) : company.toLowerCase(Locale.ROOT);
            }
        }

        String city = person.city();
        double cityRoll = random.nextDouble();
        if (cityRoll < 0.1) {
            city = "";
        } else if (cityRoll < 0.15) {
            city = pick(SampleData.CITIES); // moved
        }
        return new Contact(null, first, last, email, phone, company, city, randomDate(), person.entityId());
    }

    /** Gmail ignores dots and anything after a +, so these all reach the same inbox. */
    private String gmailVariant(String email) {
        int at = email.indexOf('@');
        String user = email.substring(0, at);
        String domain = email.substring(at);
        if (random.nextBoolean()) {
            return user + "+" + (random.nextBoolean() ? "crm" : "newsletter") + domain;
        }
        int dot = 1 + random.nextInt(Math.max(1, user.length() - 1));
        return user.substring(0, dot) + "." + user.substring(dot) + domain;
    }

    private String withSuffix(String company) {
        String suffix = pick(SampleData.COMPANY_SUFFIXES);
        return suffix.isEmpty() ? company : company + " " + suffix;
    }

    private String formatPhone(String digits) {
        return switch (random.nextInt(5)) {
            case 0 -> "+91 " + digits.substring(0, 5) + " " + digits.substring(5);
            case 1 -> "0" + digits;
            case 2 -> "+91-" + digits;
            case 3 -> digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
            default -> digits;
        };
    }

    private String uniquePhone() {
        String phone;
        do {
            phone = (6 + random.nextInt(4)) + "%09d".formatted(random.nextInt(1_000_000_000));
        } while (!usedPhones.add(phone));
        return phone;
    }

    /** Different people never share an email address, so a number is added when the name is taken. */
    private String uniqueEmail(String user, String domain) {
        String email = user + "@" + domain;
        int counter = 2;
        while (!usedEmails.add(email)) {
            email = user + counter++ + "@" + domain;
        }
        return email;
    }

    private static String emailUser(String name) {
        return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    private LocalDate randomDate() {
        return TODAY.minusDays(random.nextInt(730));
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }
}
