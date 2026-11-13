package com.debangshu.dedupe.io;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.DedupeInputException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads contacts from a CSV with a header row. Different CRMs name their columns differently
 * ("Email" vs "Email Address", "Account Name" vs "Company"), so headers are matched by a list of
 * common names, ignoring case, spaces and punctuation. Only "id" is required. Other columns are ignored.
 */
public final class ContactCsvReader {

    enum Field {
        ID("id", "recordid", "contactid", "leadid"),
        FIRST_NAME("firstname", "first", "givenname"),
        LAST_NAME("lastname", "last", "surname", "familyname"),
        EMAIL("email", "emailaddress", "mail"),
        PHONE("phone", "phonenumber", "mobile", "mobilenumber"),
        COMPANY("company", "companyname", "account", "accountname", "organization", "organisation"),
        CITY("city", "town"),
        UPDATED_AT("updatedat", "lastmodified", "modifiedtime", "modifiedat", "updated"),
        TRUE_ENTITY_ID("trueentityid");

        private final Set<String> names;

        Field(String... names) {
            this.names = Set.of(names);
        }
    }

    private ContactCsvReader() {
    }

    public static List<Contact> read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new DedupeInputException("Input file not found: " + file);
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader, file.getFileName().toString());
        }
    }

    static List<Contact> read(Reader reader, String sourceName) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        try (CSVParser parser = format.parse(reader)) {
            Map<Field, String> columns = mapColumns(parser.getHeaderNames());
            if (!columns.containsKey(Field.ID)) {
                throw new DedupeInputException(sourceName + " needs an 'id' column (found: "
                        + String.join(", ", parser.getHeaderNames()) + ")");
            }
            List<Contact> contacts = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();
            for (CSVRecord row : parser) {
                String id = value(row, columns, Field.ID);
                if (id.isEmpty()) {
                    throw new DedupeInputException(sourceName + " line " + row.getRecordNumber() + ": the id is empty");
                }
                if (!seenIds.add(id)) {
                    throw new DedupeInputException(sourceName + " line " + row.getRecordNumber() + ": id '" + id + "' appears twice");
                }
                contacts.add(new Contact(id,
                        value(row, columns, Field.FIRST_NAME),
                        value(row, columns, Field.LAST_NAME),
                        value(row, columns, Field.EMAIL),
                        value(row, columns, Field.PHONE),
                        value(row, columns, Field.COMPANY),
                        value(row, columns, Field.CITY),
                        date(value(row, columns, Field.UPDATED_AT)),
                        columns.containsKey(Field.TRUE_ENTITY_ID) ? value(row, columns, Field.TRUE_ENTITY_ID) : null));
            }
            return contacts;
        }
    }

    private static Map<Field, String> mapColumns(List<String> headers) {
        Map<Field, String> columns = new EnumMap<>(Field.class);
        for (String header : headers) {
            String key = header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            for (Field field : Field.values()) {
                if (field.names.contains(key)) {
                    columns.putIfAbsent(field, header);
                }
            }
        }
        return columns;
    }

    private static String value(CSVRecord row, Map<Field, String> columns, Field field) {
        String column = columns.get(field);
        if (column == null || !row.isSet(column)) {
            return "";
        }
        return row.get(column);
    }

    /** "2026-03-14" or "2026-03-14T10:22:00": only the date part is used. Anything else counts as unknown. */
    private static LocalDate date(String value) {
        if (value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
