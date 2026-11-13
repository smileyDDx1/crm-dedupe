package com.debangshu.dedupe.io;

import com.debangshu.dedupe.Contact;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes contacts in the same format ContactCsvReader reads (used by the generate command). */
public final class ContactCsvWriter {

    private ContactCsvWriter() {
    }

    public static void write(Path file, List<Contact> contacts) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("id", "first_name", "last_name", "email", "phone", "company", "city", "updated_at", "true_entity_id")
                .get();
        try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(file, StandardCharsets.UTF_8), format)) {
            for (Contact contact : contacts) {
                csv.printRecord(contact.id(), contact.firstName(), contact.lastName(), contact.email(), contact.phone(),
                        contact.company(), contact.city(), contact.updatedAt() == null ? "" : contact.updatedAt(),
                        contact.trueEntityId() == null ? "" : contact.trueEntityId());
            }
        }
    }
}
